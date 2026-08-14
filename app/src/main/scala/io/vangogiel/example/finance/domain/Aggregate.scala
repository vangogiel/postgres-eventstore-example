package io.vangogiel.example.finance.domain

import io.vangogiel.example.finance.domain.AccountState.OpenedState

case class Aggregate(
    accountId: String,
    state: AccountState,
    version: Long
)

object Aggregate:

  def rehydrate(events: List[Event]): Option[AccountState] =
    events.foldLeft(Option.empty[AccountState]) {
      case (None, AccountOpened(accountId)) => Some(OpenedState(accountId, balance = 0))
      case (None, _)                        => None
      case (Some(state), event)             => Some(state.transition(event))
    }

  def from(events: List[Event]): Option[Aggregate] =
    rehydrate(events).map(state => Aggregate(state.accountId, state, version = events.size.toLong))
