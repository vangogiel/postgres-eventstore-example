package io.vangogiel.example.finance.infrastructure.eventstore

import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import io.vangogiel.example.finance.application.port.{ConcurrencyConflict, EventStore}
import io.vangogiel.example.finance.domain.Event

object InMemoryEventStore:

  def apply[F[_]: Sync]: F[EventStore[F]] =
    Ref.of[F, Map[String, List[Event]]](Map.empty).map(new Impl(_))

  private class Impl[F[_]: Sync](ref: Ref[F, Map[String, List[Event]]]) extends EventStore[F]:

    def append(
        streamId: String,
        expectedVersion: Long,
        events: List[Event]
    ): F[Either[ConcurrencyConflict, Unit]] =
      ref.modify { streams =>
        val existing = streams.getOrElse(streamId, Nil)
        val actualVersion = existing.size.toLong
        if actualVersion == expectedVersion then
          streams.updated(streamId, existing ++ events) -> Right(())
        else
          streams -> Left(ConcurrencyConflict(streamId, expectedVersion, actualVersion))
      }

    def readStream(streamId: String): F[List[Event]] =
      ref.get.map(_.getOrElse(streamId, Nil))
