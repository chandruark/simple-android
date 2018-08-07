package org.simple.clinic.registration.facility

import kotlinx.android.parcel.Parcelize
import org.simple.clinic.R
import org.simple.clinic.router.screen.FullScreenKey

@Parcelize
class RegistrationLocationPermissionScreenKey : FullScreenKey {

  override fun layoutRes() = R.layout.screen_registration_location_permission
}