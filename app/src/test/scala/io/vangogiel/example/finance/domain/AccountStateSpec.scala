package io.vangogiel.example.finance.domain

import io.vangogiel.example.finance.domain.AccountState.{ClosedState, OpenedState, UnhandledTransition}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec


class AccountStateSpec extends AsyncWordSpec with Matchers :

  private val accountId = "account-1"

  "AccountState" should {
    "OpenedState.transition(AccountOpened) is idempotent" in {
      val state = OpenedState(accountId, balance = 100)
      state.transition(AccountOpened(accountId)) shouldBe state
    }

    "OpenedState.transition(AccountClosed) closes the account, preserving balance" in {
      val state = OpenedState(accountId, balance = 100)
      state.transition(AccountClosed(accountId)) shouldBe ClosedState(accountId, balance = 100)
    }

    "OpenedState.transition(MoneyDeposited) increases the balance" in {
      val state = OpenedState(accountId, balance = 100)
      state.transition(MoneyDeposited(accountId, amount = 50)) shouldBe OpenedState(accountId, balance = 150)
    }

    "OpenedState.transition(MoneyWithdrawn) decreases the balance" in {
      val state = OpenedState(accountId, balance = 100)
      state.transition(MoneyWithdrawn(accountId, amount = 30)) shouldBe OpenedState(accountId, balance = 70)
    }

    "OpenedState.transition(MoneyTransferSent) decreases the balance" in {
      val state = OpenedState(accountId, balance = 100)
      val event = MoneyTransferSent(sourceAccountId = accountId, destinationAccountId = "account-2", amount = 40)
      state.transition(event) shouldBe OpenedState(accountId, balance = 60)
    }

    "OpenedState.transition(MoneyTransferReceived) increases the balance" in {
      val state = OpenedState(accountId, balance = 100)
      val event = MoneyTransferReceived(sourceAccountId = "account-2", destinationAccountId = accountId, amount = 40)
      state.transition(event) shouldBe OpenedState(accountId, balance = 140)
    }

    "ClosedState.transition(AccountClosed) is idempotent" in {
      val state = ClosedState(accountId, balance = 100)
      state.transition(AccountClosed(accountId)) shouldBe state
    }

    "ClosedState.transition rejects any other event" in {
      val state = ClosedState(accountId, balance = 100)
      val exceptionFromDepositing = intercept[UnhandledTransition](state.transition(MoneyDeposited(accountId, amount = 10)))
      val exceptionFromReopeningAccount = intercept[UnhandledTransition](state.transition(AccountOpened(accountId)))
      exceptionFromDepositing.getMessage shouldBe "Unable to transition state=ClosedState(account-1,100) with event=MoneyDeposited(account-1,10)"
      exceptionFromReopeningAccount.getMessage shouldBe "Unable to transition state=ClosedState(account-1,100) with event=AccountOpened(account-1)"
    }
  }
