package io.vangogiel.example.finance.domain

sealed trait AccountState:
  def accountId: String
  def balance: BigDecimal
  def transition(event: Event): AccountState

object AccountState:

  case class UnhandledTransition(state: AccountState, event: Event)
      extends RuntimeException(s"Unable to transition state=$state with event=$event")

  case class OpenedState(accountId: String, balance: BigDecimal) extends AccountState:
    def transition(event: Event): AccountState = event match
      case AccountOpened(_)                    => this
      case AccountClosed(_)                    => ClosedState(accountId, balance)
      case MoneyDeposited(_, amount)           => copy(balance = balance + amount)
      case MoneyWithdrawn(_, amount)           => copy(balance = balance - amount)
      case MoneyTransferSent(_, _, amount)     => copy(balance = balance - amount)
      case MoneyTransferReceived(_, _, amount) => copy(balance = balance + amount)

  case class ClosedState(accountId: String, balance: BigDecimal) extends AccountState:
    def transition(event: Event): AccountState = event match
      case AccountClosed(_) => this
      case _                => throw UnhandledTransition(this, event)
