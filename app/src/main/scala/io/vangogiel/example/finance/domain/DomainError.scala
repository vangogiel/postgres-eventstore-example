package io.vangogiel.example.finance.domain

sealed trait DomainError

case class AccountNotFound(accountId: String) extends DomainError
case class AccountAlreadyExists(accountId: String) extends DomainError
case class AccountAlreadyClosed(accountId: String) extends DomainError

case class InsufficientFunds(accountId: String, balance: BigDecimal, amount: BigDecimal)
    extends DomainError

case class ConcurrentModification(accountId: String) extends DomainError
