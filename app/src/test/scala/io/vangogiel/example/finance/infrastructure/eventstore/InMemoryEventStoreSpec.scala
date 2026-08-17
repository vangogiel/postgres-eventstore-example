package io.vangogiel.example.finance.infrastructure.eventstore

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.vangogiel.example.finance.application.port.ConcurrencyConflict
import io.vangogiel.example.finance.domain.{AccountClosed, AccountOpened, MoneyDeposited}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryEventStoreSpec extends AnyWordSpec with Matchers:

  private val streamId = "account-1"

  "InMemoryEventStore" should {

    "read an empty list for a stream that was never appended to" in {
      val events = InMemoryEventStore[IO].flatMap(_.readStream(streamId)).unsafeRunSync()
      events shouldBe Nil
    }

    "append to a new stream at expected version 0" in {
      val result = for
        store <- InMemoryEventStore[IO]
        appendResult <- store.append(streamId, expectedVersion = 0, List(AccountOpened(streamId)))
        events <- store.readStream(streamId)
      yield (appendResult, events)

      val (appendResult, events) = result.unsafeRunSync()
      appendResult shouldBe Right(())
      events shouldBe List(AccountOpened(streamId))
    }

    "append subsequent events at the stream's current version" in {
      val result = for
        store <- InMemoryEventStore[IO]
        _ <- store.append(streamId, expectedVersion = 0, List(AccountOpened(streamId)))
        appendResult <- store.append(
          streamId,
          expectedVersion = 1,
          List(MoneyDeposited(streamId, amount = 50))
        )
        events <- store.readStream(streamId)
      yield (appendResult, events)

      val (appendResult, events) = result.unsafeRunSync()
      appendResult shouldBe Right(())
      events shouldBe List(AccountOpened(streamId), MoneyDeposited(streamId, amount = 50))
    }

    "reject an append with a stale expected version" in {
      val result = for
        store <- InMemoryEventStore[IO]
        _ <- store.append(streamId, expectedVersion = 0, List(AccountOpened(streamId)))
        appendResult <- store.append(streamId, expectedVersion = 0, List(AccountClosed(streamId)))
        events <- store.readStream(streamId)
      yield (appendResult, events)

      val (appendResult, events) = result.unsafeRunSync()
      appendResult shouldBe Left(ConcurrencyConflict(streamId, expectedVersion = 0, actualVersion = 1))
      events shouldBe List(AccountOpened(streamId))
    }

    "keep streams independent from one another" in {
      val otherStreamId = "account-2"
      val result = for
        store <- InMemoryEventStore[IO]
        _ <- store.append(streamId, expectedVersion = 0, List(AccountOpened(streamId)))
        _ <- store.append(otherStreamId, expectedVersion = 0, List(AccountOpened(otherStreamId)))
        firstStreamEvents <- store.readStream(streamId)
        secondStreamEvents <- store.readStream(otherStreamId)
      yield (firstStreamEvents, secondStreamEvents)

      val (firstStreamEvents, secondStreamEvents) = result.unsafeRunSync()
      firstStreamEvents shouldBe List(AccountOpened(streamId))
      secondStreamEvents shouldBe List(AccountOpened(otherStreamId))
    }
  }
