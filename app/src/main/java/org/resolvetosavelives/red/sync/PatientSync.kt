package org.resolvetosavelives.red.sync

import com.f2prateek.rx.preferences2.Preference
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toCompletable
import io.reactivex.schedulers.Schedulers.single
import org.resolvetosavelives.red.newentry.search.PatientRepository
import org.resolvetosavelives.red.newentry.search.PatientWithAddress
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PatientSync @Inject constructor(
    private val api: PatientSyncApiV1,
    private val repository: PatientRepository,
    private val configProvider: Single<PatientSyncConfig>,
    @Named("last_pull_timestamp") private val lastPullTimestamp: Preference<Optional<Instant>>
) {

  fun sync(): Completable {
    return Completable.mergeArrayDelayError(push(), pull())
  }

  fun push(): Completable {
    val cachedPatients = repository
        .patientsWithSyncStatus(SyncStatus.PENDING)
        .cache()

    val pushResult = cachedPatients
        // Converting to an Observable because Single#filter() returns a Maybe.
        .toObservable()
        .filter({ it.isNotEmpty() })
        .map { patients -> patients.map { it.toPayload() } }
        .map(::PatientPushRequest)
        .flatMapSingle { request -> api.push(request) }
        .flatMapSingle {
          when {
            it.hasValidationErrors() -> Single.just(FailedWithValidationErrors(it.validationErrors))
            else -> cachedPatients.map(::Pushed)
          }
        }

    return pushResult
        .flatMapCompletable { result ->
          when (result) {
            is Pushed -> repository.markPatientsAsSynced(result.syncedPatients)
            is FailedWithValidationErrors -> logValidationErrors(result.errors)
          }
        }
  }

  private fun logValidationErrors(errors: List<ValidationErrors>?): Completable? {
    return { Timber.e("Server sent validation errors for patients: $errors") }.toCompletable()
  }

  fun pull(): Completable {
    return configProvider
        .flatMapCompletable { config ->
          lastPullTimestamp.asObservable()
              .take(1)
              .flatMapSingle { lastPullTime ->
                when (lastPullTime) {
                  is Some -> api.pull(recordsToRetrieve = config.batchSize, latestRecordTimestamp = lastPullTime.value)
                  is None -> api.pull(recordsToRetrieve = config.batchSize, isFirstSync = true)
                }
              }
              .flatMap { response ->
                repository.mergeWithLocalData(response.patients)
                    .observeOn(single())
                    .andThen({ lastPullTimestamp.set(Some(response.latestRecordTimestamp)) }.toCompletable())
                    .andThen(Observable.just(response))
              }
              .repeat()
              .takeWhile({ response -> response.patients.size >= config.batchSize })
              .ignoreElements()
        }
  }
}

sealed class PatientPullResult

data class Pushed(val syncedPatients: List<PatientWithAddress>) : PatientPullResult()

data class FailedWithValidationErrors(val errors: List<ValidationErrors>?) : PatientPullResult()