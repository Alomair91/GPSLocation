/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.omairtech.gpslocation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.model.UiData
import com.omairtech.gpslocation.util.LocationType
import com.omairtech.gpslocation.util.fullScreen
import com.omairtech.gpslocation.util.hasPermission
import com.omairtech.gpslocation.util.requestPermissionWithRationale

private const val TAG = "PermissionRequestFrag"

/**
 * Displays information about why a user should enable either the fine location permission or the
 * background location permission (depending on what is needed).
 *
 * Allows users to grant the permissions as well.
 */
internal class PermissionFragment : BottomSheetDialogFragment() {

    // Type of permission to request (fine or background). Set by calling Activity.
    private var locationType: LocationType? = null

    // To change ui data
    private var permissionUIData: PermissionUIData? = null

    // Must implement this callbacks to handle response
    private var callback: ((Boolean) -> Unit?)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
        locationType = arguments?.getSerializable(PERMISSION_REQUEST_TYPE) as LocationType
        permissionUIData = arguments?.getSerializable(PERMISSION_UI_DATA) as PermissionUIData
    }

    private lateinit var root: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        root = inflater.inflate(R.layout.fragment_permission, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root.apply {
            if (locationType == LocationType.FINE_LOCATION)
                setUi(this, permissionUIData?.foreground)
            else if (locationType == LocationType.BACKGROUND_LOCATION)
                setUi(this, permissionUIData?.background)

            findViewById<TextView>(R.id.btn_approve).setOnClickListener {
                if (locationType == LocationType.FINE_LOCATION)
                    requestFineLocationPermission()
                else if (locationType == LocationType.BACKGROUND_LOCATION)
                    requestBackgroundLocationPermission()
            }
            findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
                setResult(false)
            }
        }
        // This callback will only be called when Back button pressed.
        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) setResult(false)
            true
        }
    }

    private fun setUi(root: View, data: UiData?) {
        data?.let {
            if (data.hideUI == true) {
                root.findViewById<ImageView>(R.id.image_icon).visibility = View.GONE
                root.findViewById<TextView>(R.id.txt_title).visibility = View.GONE
                root.findViewById<TextView>(R.id.tdt_details).visibility = View.GONE
            } else {
                root.findViewById<ImageView>(R.id.image_icon).setImageResource(it.icon_res!!)
                root.findViewById<TextView>(R.id.txt_title).text = getString(it.title_res!!)
                root.findViewById<TextView>(R.id.tdt_details).text = getString(it.details_res!!)
            }
            root.findViewById<TextView>(R.id.btn_approve).text = getString(it.approve_res!!)
            root.findViewById<TextView>(R.id.btn_cancel).text = getString(it.cancel_res!!)
        }
    }

    override fun onStart() {
        super.onStart()
        fullScreen()
    }

    private fun requestFineLocationPermission() {
        val permissionApproved =
            context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ?: return

        if (permissionApproved) {
            setResult(true)
        } else {
            requestLocationPermission(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), fineLocationRationalSnackbar)
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) == false) {
            requestFineLocationPermission()
            return
        }

        val permissionApproved =
            context?.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ?: return

        if (permissionApproved) {
            setResult(true)
        } else {
            requestLocationPermission(arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ), backgroundRationalSnackbar)
        }
    }

    private fun requestLocationPermission(permissions: Array<String>, snackbar: Snackbar? = null) {
        requestPermissionWithRationale(permissions, requestPermissionLauncher, snackbar)
    }

    // Handle Permissions result (granted/rejected)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "onRequestPermissionResult")
        when {
            permissions.entries.isEmpty() -> {
                // If user interaction was interrupted, the permission request
                // is cancelled and you receive an empty array.
                Log.d(TAG, "User interaction was cancelled.")
            }

            permissions.entries.first().value -> {
                if ((permissions.entries.first().key == Manifest.permission.ACCESS_COARSE_LOCATION
                            || permissions.entries.first().key == Manifest.permission.ACCESS_FINE_LOCATION)
                    && locationType == LocationType.BACKGROUND_LOCATION
                ) requestBackgroundLocationPermission()
                else setResult(true)
            }

            else -> {
                val permissionDeniedExplanation =
                    if (permissions.entries.first().key == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                        permissionUIData?.foreground?.snackbar_permission_denied_explanation_res!!
                    } else {
                        permissionUIData?.background?.snackbar_permission_denied_explanation_res!!
                    }

                Snackbar
                    .make(dialog?.window?.decorView!!,
                        permissionDeniedExplanation, Snackbar.LENGTH_LONG)
                    .setAction(R.string.settings) {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", requireActivity().packageName, null)
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }


    // If the user denied a previous permission request, but didn't check "Don't ask again", these
    // Snackbars provided an explanation for why user should approve, i.e., the additional
    // rationale.
    private val fineLocationRationalSnackbar by lazy {
        Snackbar
            .make(
                dialog?.window?.decorView!!,
                permissionUIData?.foreground?.snackbar_permission_rationale_res!!,
                Snackbar.LENGTH_LONG
            )
            .setAction(android.R.string.ok) {
                requestLocationPermission(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
    }
    private val backgroundRationalSnackbar by lazy {
        Snackbar
            .make(
                dialog?.window?.decorView!!,
                permissionUIData?.background?.snackbar_permission_rationale_res!!,
                Snackbar.LENGTH_LONG
            )
            .setAction(android.R.string.ok) {
                requestLocationPermission(arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ))
            }
    }

    private fun setResult(result: Boolean) {
        callback?.let { it(result) }
        dismiss()
    }

    companion object {
        private const val PERMISSION_UI_DATA = "PERMISSION_UI_DATA"
        private const val PERMISSION_REQUEST_TYPE = "PERMISSION_REQUEST_TYPE"

        /**
         * Use this factory method to create a new instance of this fragment using the provided parameters.
         *
         * @return A new instance of PermissionFragment.
         */
        @JvmStatic
        fun newInstance(
            context: Context,
            locationType: LocationType,
            permissionUIData: PermissionUIData?,
            callback: (Boolean) -> Unit,
        ) = PermissionFragment().apply {
            arguments = Bundle().apply {
                putSerializable(PERMISSION_UI_DATA, permissionUIData)
                putSerializable(PERMISSION_REQUEST_TYPE, locationType)
            }
            this.callback = callback
            isCancelable = false
            show((context as FragmentActivity).supportFragmentManager, null)
        }
    }
}
