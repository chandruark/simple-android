package org.simple.clinic.phone

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class TwilioPhoneNumberMaskerTest {

  private lateinit var masker: PhoneNumberMasker

  private lateinit var config: PhoneNumberMaskerConfig

  @Before
  fun setUp() {
    masker = TwilioPhoneNumberMasker(Single.fromCallable { config }, mock())
  }

  @Test
  fun `when masking is disabled then plain phone numbers should be called`() {
    config = PhoneNumberMaskerConfig(maskingEnabled = false)

    val callerCaptor = CallerCaptor()
    val plainNumber = "123"

    masker.maskAndCall(plainNumber, caller = callerCaptor).blockingAwait()

    assertThat(callerCaptor.calledNumber).isEqualTo(plainNumber)
  }

  class CallerCaptor : Caller {
    lateinit var calledNumber: String

    override fun call(context: Context, phoneNumber: String) {
      calledNumber = phoneNumber
    }
  }
}
