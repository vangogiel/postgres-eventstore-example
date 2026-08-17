package io.vangogiel.example.finance.application

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import io.vangogiel.example.finance.domain.*
import io.vangogiel.example.finance.infrastructure.eventstore.InMemoryEventStore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AccountServiceSpec extends AnyWordSpec with Matchers:

  private val accountId = "account-1"
  private val otherAccountId = "account-2"

  private def newService: IO[AccountService[IO]] =
    InMemoryEventStore[IO].map(new AccountService[IO](_))

  "AccountService.handle" should {

    "open a new account" in {
      val result = for
        service <- newService
        outcome <- service.handle(accountId, OpenAccount(accountId))
      yield outcome

      result.unsafeRunSync() shouldBe Right(List(AccountOpened(accountId)))
    }

    "fail to open an account that already exists" in {
      val result = for
        service <- newService
        _ <- service.handle(accountId, OpenAccount(accountId))
        outcome <- service.handle(accountId, OpenAccount(accountId))
      yield outcome

      result.unsafeRunSync() shouldBe Left(AccountAlreadyExists(accountId))
    }

    "deposit into an open account, reflecting the balance from persisted events" in {
      val result = for
        service <- newService
        _ <- service.handle(accountId, OpenAccount(accountId))
        outcome <- service.handle(accountId, DepositMoney(accountId, amount = 100))
      yield outcome

      result.unsafeRunSync() shouldBe Right(List(MoneyDeposited(accountId, amount = 100)))
    }

    "fail to deposit into an account that does not exist" in {
      val result = for
        service <- newService
        outcome <- service.handle(accountId, DepositMoney(accountId, amount = 100))
      yield outcome

      result.unsafeRunSync() shouldBe Left(AccountNotFound(accountId))
    }

    "fail to withdraw more than the balance built up from prior events" in {
      val result = for
        service <- newService
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, DepositMoney(accountId, amount = 30))
        outcome <- service.handle(accountId, WithdrawMoney(accountId, amount = 50))
      yield outcome

      result.unsafeRunSync() shouldBe Left(InsufficientFunds(accountId, balance = 30, amount = 50))
    }

    "fail to act on a closed account" in {
      val result = for
        service <- newService
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, CloseAccount(accountId))
        outcome <- service.handle(accountId, DepositMoney(accountId, amount = 10))
      yield outcome

      result.unsafeRunSync() shouldBe Left(AccountAlreadyClosed(accountId))
    }

    "persist events across calls, keeping the stream as the source of truth" in {
      val result = for
        service <- newService
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, DepositMoney(accountId, amount = 100))
        _ <- service.handle(accountId, WithdrawMoney(accountId, amount = 40))
        _ <- service.handle(otherAccountId, OpenAccount(otherAccountId))
        outcome <- service.handle(accountId, TransferMoney(accountId, otherAccountId, amount = 20))
      yield outcome

      result.unsafeRunSync() shouldBe
        Right(List(MoneyTransferSent(accountId, otherAccountId, amount = 20)))
    }

    "credit the destination account when a transfer succeeds" in {
      val result = for
        store <- InMemoryEventStore[IO]
        service = new AccountService[IO](store)
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, DepositMoney(accountId, amount = 100))
        _ <- service.handle(otherAccountId, OpenAccount(otherAccountId))
        _ <- service.handle(accountId, TransferMoney(accountId, otherAccountId, amount = 20))
        sourceState <- service.currentState(accountId)
        destinationState <- service.currentState(otherAccountId)
      yield (sourceState, destinationState)

      val (sourceState, destinationState) = result.unsafeRunSync()
      sourceState.map(_.balance) shouldBe Some(BigDecimal(80))
      destinationState.map(_.balance) shouldBe Some(BigDecimal(20))
    }

    "fail a transfer to an account that does not exist, without debiting the source" in {
      val result = for
        store <- InMemoryEventStore[IO]
        service = new AccountService[IO](store)
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, DepositMoney(accountId, amount = 100))
        outcome <- service.handle(accountId, TransferMoney(accountId, otherAccountId, amount = 20))
        sourceState <- service.currentState(accountId)
      yield (outcome, sourceState)

      val (outcome, sourceState) = result.unsafeRunSync()
      outcome shouldBe Left(AccountNotFound(otherAccountId))
      sourceState.map(_.balance) shouldBe Some(BigDecimal(100))
    }

    "fail a transfer to a closed account, without debiting the source" in {
      val result = for
        store <- InMemoryEventStore[IO]
        service = new AccountService[IO](store)
        _ <- service.handle(accountId, OpenAccount(accountId))
        _ <- service.handle(accountId, DepositMoney(accountId, amount = 100))
        _ <- service.handle(otherAccountId, OpenAccount(otherAccountId))
        _ <- service.handle(otherAccountId, CloseAccount(otherAccountId))
        outcome <- service.handle(accountId, TransferMoney(accountId, otherAccountId, amount = 20))
        sourceState <- service.currentState(accountId)
      yield (outcome, sourceState)

      val (outcome, sourceState) = result.unsafeRunSync()
      outcome shouldBe Left(AccountAlreadyClosed(otherAccountId))
      sourceState.map(_.balance) shouldBe Some(BigDecimal(100))
    }

    "let exactly one of two racing commands against a fresh account succeed" in {
      val result = for
        store <- InMemoryEventStore[IO]
        service = new AccountService[IO](store)
        outcomes <- (
          service.handle(accountId, OpenAccount(accountId)),
          service.handle(accountId, OpenAccount(accountId))
        ).parTupled
        events <- store.readStream(accountId)
      yield (outcomes, events)

      val ((first, second), events) = result.unsafeRunSync()
      val outcomes = List(first, second)

      outcomes.count(_ == Right(List(AccountOpened(accountId)))) shouldBe 1
      outcomes.count {
        case Left(AccountAlreadyExists(`accountId`))   => true
        case Left(ConcurrentModification(`accountId`)) => true
        case _                                         => false
      } shouldBe 1
      events shouldBe List(AccountOpened(accountId))
    }
  }
