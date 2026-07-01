package io.vangogiel.example.finance.domain

trait AccountState:
  def transition(event: Event): AccountState

object AccountState:

  case class UnhandledTransition(state: AccountState, event: Event)
      extends RuntimeException(s"Unable to transition state=$state with event=$event")

  case class OpenedState(accountId: String, balance: BigDecimal) extends AccountState:
    def transition(event: Event): AccountState = event match
      case AccountOpened => this
      case AccountClosed => ClosedState(accountId, balance)
      case _             => throw UnhandledTransition(this, event)

  case class ClosedState(accountId: String, balance: BigDecimal) extends AccountState:
    def transition(event: Event): AccountState = event match
      case AccountOpened => OpenedState(accountId, balance)
      case AccountClosed => this
      case _             => throw UnhandledTransition(this, event)
