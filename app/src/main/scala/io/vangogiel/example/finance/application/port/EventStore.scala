package io.vangogiel.example.finance.application.port

import io.vangogiel.example.finance.domain.Event

trait EventStore[F[_]]:
  def append(
      streamId: String,
      expectedVersion: Long,
      events: List[Event]
  ): F[Either[ConcurrencyConflict, Unit]]

  def readStream(streamId: String): F[List[Event]]
