package org.simple.clinic.analytics

class MockReporter : Reporter {

  val receivedEvents = mutableListOf<Event>()
  val setProperties = mutableMapOf<String, Any>()

  override fun createEvent(event: String, props: Map<String, Any>) {
    receivedEvents.add(Event(event, props))
  }

  override fun setProperty(key: String, value: Any) {
    setProperties[key] = value
  }

  fun clearReceivedEvents() {
    receivedEvents.clear()
  }

  fun clearSetProperties() {
    setProperties.clear()
  }

  data class Event(val name: String, val props: Map<String, Any>)
}