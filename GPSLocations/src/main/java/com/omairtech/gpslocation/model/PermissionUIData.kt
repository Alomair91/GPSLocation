package com.omairtech.gpslocation.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.omairtech.gpslocation.R
import java.io.Serializable

@Keep
abstract class UiData(
    val hideUI: Boolean? = false,
    @DrawableRes val icon_res: Int?,
    @StringRes val title_res: Int?,
    @StringRes val details_res: Int?,
    @StringRes val approve_res: Int?,
    @StringRes val cancel_res: Int?,
    @StringRes val snackbar_permission_rationale_res: Int?,
    @StringRes val snackbar_permission_denied_explanation_res: Int?,
)

@Keep
data class PermissionUIData(
    val foreground: Foreground? = Foreground(),
    val background: Background? = Background()
) : Serializable {
    data class Foreground(
        val hideUi: Boolean? = false,
        @DrawableRes val icon: Int? = R.drawable.ic_location_on_24px,
        @StringRes val title: Int? = R.string.fine_location_access_rationale_title,
        @StringRes val details: Int? = R.string.fine_location_access_rationale_details,
        @StringRes val btn_approve: Int? = R.string.approve_location_access,
        @StringRes var btn_cancel: Int? = android.R.string.cancel,
        @StringRes val snackbar_permission_rationale: Int? = R.string.fine_location_permission_rationale,
        @StringRes val snackbar_permission_denied_explanation: Int? = R.string.fine_permission_denied_explanation,
    ) : UiData(hideUi,icon, title, details, btn_approve, btn_cancel,
        snackbar_permission_rationale,snackbar_permission_denied_explanation)

    data class Background(
        val hideUi: Boolean? = false,
        @DrawableRes val icon: Int? = R.drawable.ic_my_location_24px,
        @StringRes val title: Int? = R.string.background_location_access_rationale_title,
        @StringRes val details: Int? = R.string.background_location_access_rationale_details,
        @StringRes val btn_approve: Int? = R.string.approve_background_location_access,
        @StringRes val btn_cancel: Int? = android.R.string.cancel,
        @StringRes val snackbar_permission_rationale: Int? = R.string.background_location_permission_rationale,
        @StringRes val snackbar_permission_denied_explanation: Int? = R.string.background_permission_denied_explanation,
    ) : UiData(hideUi,icon, title, details, btn_approve, btn_cancel,
        snackbar_permission_rationale,snackbar_permission_denied_explanation)
}