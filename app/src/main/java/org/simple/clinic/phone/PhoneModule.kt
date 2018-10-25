package org.simple.clinic.phone

import dagger.Module
import dagger.Provides
import io.reactivex.Single

@Module
class PhoneModule {

  @Provides
  fun config() = Single.just(PhoneNumberMaskerConfig(maskingEnabled = false))!!

  @Provides
  fun masker(twilioMasker: TwilioPhoneNumberMasker): PhoneNumberMasker = twilioMasker
}
