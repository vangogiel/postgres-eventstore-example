package io.vangogiel.example.finance.domain

sealed trait Command

case class OpenAccount(accountId: String) extends Command
case class CloseAccount(accountId: String) extends Command

case class DepositMoney(accountId: String, amount: BigDecimal) extends Command
case class WithdrawMoney(accountId: String, amount: BigDecimal) extends Command

case class TransferMoney(
    sourceAccountId: String,
    destinationAccountId: String,
    amount: BigDecimal
) extends Command
