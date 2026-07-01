package io.vangogiel.example.finance.domain

case class Aggregate(
    accountId: String,
    state: AccountState,
    version: Long
)
