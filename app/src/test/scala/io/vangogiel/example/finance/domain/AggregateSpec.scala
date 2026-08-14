package io.vangogiel.example.finance.domain

import io.vangogiel.example.finance.domain.AccountState.{ClosedState, OpenedState, UnhandledTransition}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class AggregateSpec extends AsyncWordSpec with Matchers:

  private val accountId = "account-1"

  "Aggregate.rehydrate" should {
    "return None for an empty event list" in {
      Aggregate.rehydrate(Nil) shouldBe None
    }

    "return None when the first event is not AccountOpened" in {
      Aggregate.rehydrate(List(MoneyDeposited(accountId, amount = 50))) shouldBe None
    }

    "open the account from an AccountOpened event" in {
      Aggregate.rehydrate(List(AccountOpened(accountId))) shouldBe
        Some(OpenedState(accountId, balance = 0))
    }

    "fold money movement events into the balance" in {
      val events = List(
        AccountOpened(accountId),
        MoneyDeposited(accountId, amount = 100),
        MoneyWithdrawn(accountId, amount = 30),
        MoneyTransferSent(accountId, "account-2", amount = 20),
        MoneyTransferReceived("account-2", accountId, amount = 10)
      )
      Aggregate.rehydrate(events) shouldBe Some(OpenedState(accountId, balance = 60))
    }

    "close the account from an AccountClosed event" in {
      val events = List(
        AccountOpened(accountId),
        MoneyDeposited(accountId, amount = 50),
        AccountClosed(accountId)
      )
      Aggregate.rehydrate(events) shouldBe Some(ClosedState(accountId, balance = 50))
    }

    "throw when an event is applied after the account is closed" in {
      val events = List(
        AccountOpened(accountId),
        AccountClosed(accountId),
        MoneyDeposited(accountId, amount = 50)
      )
      val exception = intercept[UnhandledTransition](Aggregate.rehydrate(events))
      exception.event shouldBe MoneyDeposited(accountId, amount = 50)
    }
  }

  "Aggregate.from" should {
    "return None for an empty event list" in {
      Aggregate.from(Nil) shouldBe None
    }

    "build an aggregate with a version equal to the number of events applied" in {
      val events = List(
        AccountOpened(accountId),
        MoneyDeposited(accountId, amount = 100),
        MoneyWithdrawn(accountId, amount = 30)
      )
      Aggregate.from(events) shouldBe
        Some(Aggregate(accountId, OpenedState(accountId, balance = 70), version = 3))
    }
  }
