package com.omairtech.gpslocation.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.omairtech.gpslocation.R
import java.io.Serializable

@Keep
abstract class UiData(
    var hideUI: Boolean? = false,
    @DrawableRes var icon_res: Int?,
    @StringRes var title_res: Int?,
    @StringRes var details_res: Int?,
    @StringRes var approve_res: Int?,
    @StringRes var cancel_res: Int?,
    @StringRes var snackbar_permission_rationale_res: Int?,
    @StringRes var snackbar_permission_denied_explanation_res: Int?,
): Serializable

@Keep
data class PermissionUIData(
    var foreground: Foreground? = Foreground(),
    var background: Background? = Background()
) : Serializable {
    data class Foreground(
        var hideUi: Boolean? = false,
        @DrawableRes var icon: Int? = R.drawable.ic_location_on_24px,
        @StringRes var title: Int? = R.string.fine_location_access_rationale_title,
        @StringRes var details: Int? = R.string.fine_location_access_rationale_details,
        @StringRes var btn_approve: Int? = R.string.approve_location_access,
        @StringRes var btn_cancel: Int? = android.R.string.cancel,
        @StringRes var snackbar_permission_rationale: Int? = R.string.fine_location_permission_rationale,
        @StringRes var snackbar_permission_denied_explanation: Int? = R.string.fine_permission_denied_explanation,
    ) : UiData(hideUi,icon, title, details, btn_approve, btn_cancel,
        snackbar_permission_rationale,snackbar_permission_denied_explanation)

    data class Background(
        var hideUi: Boolean? = false,
        @DrawableRes var icon: Int? = R.drawable.ic_my_location_24px,
        @StringRes var title: Int? = R.string.background_location_access_rationale_title,
        @StringRes var details: Int? = R.string.background_location_access_rationale_details,
        @StringRes var btn_approve: Int? = R.string.approve_background_location_access,
        @StringRes var btn_cancel: Int? = android.R.string.cancel,
        @StringRes var snackbar_permission_rationale: Int? = R.string.background_location_permission_rationale,
        @StringRes var snackbar_permission_denied_explanation: Int? = R.string.background_permission_denied_explanation,
    ) : UiData(hideUi,icon, title, details, btn_approve, btn_cancel,
        snackbar_permission_rationale,snackbar_permission_denied_explanation)
}