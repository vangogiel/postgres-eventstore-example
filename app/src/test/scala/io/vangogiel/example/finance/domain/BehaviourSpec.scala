package io.vangogiel.example.finance.domain

import io.vangogiel.example.finance.domain.AccountState.{ClosedState, OpenedState}
import io.vangogiel.example.finance.domain.Behaviour.decide
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class BehaviourSpec extends AsyncWordSpec with Matchers:

  private val accountId = "account-1"
  private val otherAccountId = "account-2"

  "Behaviour" should {

    "OpenAccount" should {
      "open the account when no state exists" in {
        decide(None, OpenAccount(accountId)) shouldBe Right(List(AccountOpened(accountId)))
      }

      "fail when the account is already open" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), OpenAccount(accountId)) shouldBe Left(AccountAlreadyExists(accountId))
      }

      "fail when the account is already closed" in {
        val state = ClosedState(accountId, balance = 0)
        decide(Some(state), OpenAccount(accountId)) shouldBe Left(AccountAlreadyExists(accountId))
      }
    }

    "CloseAccount" should {
      "fail when the account does not exist" in {
        decide(None, CloseAccount(accountId)) shouldBe Left(AccountNotFound(accountId))
      }

      "close an open account" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), CloseAccount(accountId)) shouldBe Right(List(AccountClosed(accountId)))
      }

      "fail when the account is already closed" in {
        val state = ClosedState(accountId, balance = 0)
        decide(Some(state), CloseAccount(accountId)) shouldBe Left(AccountAlreadyClosed(accountId))
      }
    }

    "DepositMoney" should {
      "fail when the account does not exist" in {
        decide(None, DepositMoney(accountId, amount = 50)) shouldBe Left(AccountNotFound(accountId))
      }

      "deposit into an open account" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), DepositMoney(accountId, amount = 50)) shouldBe
          Right(List(MoneyDeposited(accountId, amount = 50)))
      }

      "fail when the account is closed" in {
        val state = ClosedState(accountId, balance = 0)
        decide(Some(state), DepositMoney(accountId, amount = 50)) shouldBe
          Left(AccountAlreadyClosed(accountId))
      }
    }

    "WithdrawMoney" should {
      "fail when the account does not exist" in {
        decide(None, WithdrawMoney(accountId, amount = 50)) shouldBe Left(AccountNotFound(accountId))
      }

      "withdraw from an open account with sufficient funds" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), WithdrawMoney(accountId, amount = 50)) shouldBe
          Right(List(MoneyWithdrawn(accountId, amount = 50)))
      }

      "allow withdrawing the exact balance" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), WithdrawMoney(accountId, amount = 100)) shouldBe
          Right(List(MoneyWithdrawn(accountId, amount = 100)))
      }

      "fail when funds are insufficient" in {
        val state = OpenedState(accountId, balance = 30)
        decide(Some(state), WithdrawMoney(accountId, amount = 50)) shouldBe
          Left(InsufficientFunds(accountId, balance = 30, amount = 50))
      }

      "fail when the account is closed" in {
        val state = ClosedState(accountId, balance = 0)
        decide(Some(state), WithdrawMoney(accountId, amount = 50)) shouldBe
          Left(AccountAlreadyClosed(accountId))
      }
    }

    "TransferMoney" should {
      "fail when the source account does not exist" in {
        decide(None, TransferMoney(accountId, otherAccountId, amount = 50)) shouldBe
          Left(AccountNotFound(accountId))
      }

      "transfer from an open account with sufficient funds" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), TransferMoney(accountId, otherAccountId, amount = 50)) shouldBe
          Right(List(MoneyTransferSent(accountId, otherAccountId, amount = 50)))
      }

      "allow transferring the exact balance" in {
        val state = OpenedState(accountId, balance = 100)
        decide(Some(state), TransferMoney(accountId, otherAccountId, amount = 100)) shouldBe
          Right(List(MoneyTransferSent(accountId, otherAccountId, amount = 100)))
      }

      "fail when funds are insufficient" in {
        val state = OpenedState(accountId, balance = 30)
        decide(Some(state), TransferMoney(accountId, otherAccountId, amount = 50)) shouldBe
          Left(InsufficientFunds(accountId, balance = 30, amount = 50))
      }

      "fail when the source account is closed" in {
        val state = ClosedState(accountId, balance = 0)
        decide(Some(state), TransferMoney(accountId, otherAccountId, amount = 50)) shouldBe
          Left(AccountAlreadyClosed(accountId))
      }
    }
  }
