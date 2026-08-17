package io.vangogiel.example.finance.application.port

case class ConcurrencyConflict(streamId: String, expectedVersion: Long, actualVersion: Long)
