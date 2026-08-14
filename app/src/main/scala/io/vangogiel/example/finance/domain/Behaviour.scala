package io.vangogiel.example.finance.domain

import io.vangogiel.example.finance.domain.AccountState.{ClosedState, OpenedState}

object Behaviour:

  def decide(state: Option[AccountState], command: Command): Either[DomainError, List[Event]] =
    (state, command) match

      case (None, OpenAccount(accountId)) =>
        Right(List(AccountOpened(accountId)))
      case (Some(_), OpenAccount(accountId)) =>
        Left(AccountAlreadyExists(accountId))

      case (None, CloseAccount(accountId)) =>
        Left(AccountNotFound(accountId))
      case (Some(OpenedState(accountId, _)), CloseAccount(_)) =>
        Right(List(AccountClosed(accountId)))
      case (Some(ClosedState(accountId, _)), CloseAccount(_)) =>
        Left(AccountAlreadyClosed(accountId))

      case (None, DepositMoney(accountId, _)) =>
        Left(AccountNotFound(accountId))
      case (Some(OpenedState(accountId, _)), DepositMoney(_, amount)) =>
        Right(List(MoneyDeposited(accountId, amount)))
      case (Some(ClosedState(accountId, _)), DepositMoney(_, _)) =>
        Left(AccountAlreadyClosed(accountId))

      case (None, WithdrawMoney(accountId, _)) =>
        Left(AccountNotFound(accountId))
      case (Some(OpenedState(accountId, balance)), WithdrawMoney(_, amount)) =>
        if balance >= amount then Right(List(MoneyWithdrawn(accountId, amount)))
        else Left(InsufficientFunds(accountId, balance, amount))
      case (Some(ClosedState(accountId, _)), WithdrawMoney(_, _)) =>
        Left(AccountAlreadyClosed(accountId))

      case (None, TransferMoney(sourceAccountId, _, _)) =>
        Left(AccountNotFound(sourceAccountId))
      case (
            Some(OpenedState(sourceAccountId, balance)),
            TransferMoney(_, destinationAccountId, amount)
          ) =>
        if balance >= amount then
          Right(List(MoneyTransferSent(sourceAccountId, destinationAccountId, amount)))
        else Left(InsufficientFunds(sourceAccountId, balance, amount))
      case (Some(ClosedState(sourceAccountId, _)), TransferMoney(_, _, _)) =>
        Left(AccountAlreadyClosed(sourceAccountId))
