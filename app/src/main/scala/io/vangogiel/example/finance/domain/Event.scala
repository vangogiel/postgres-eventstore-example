package io.vangogiel.example.finance.domain

sealed trait Event

case class AccountOpened(accountId: String) extends Event
case class AccountClosed(accountId: String) extends Event

case class MoneyDeposited(accountId: String, amount: BigDecimal) extends Event
case class MoneyWithdrawn(accountId: String, amount: BigDecimal) extends Event

case class MoneyTransferSent(
    sourceAccountId: String,
    destinationAccountId: String,
    amount: BigDecimal
) extends Event

case class MoneyTransferReceived(
    sourceAccountId: String,
    destinationAccountId: String,
    amount: BigDecimal
) extends Event
