package org.simple.clinic.phone

import io.reactivex.Completable

interface PhoneNumberMasker {
  fun maskAndCall(numberToMask: String, caller: Caller): Completable
}
