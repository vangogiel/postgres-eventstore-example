package io.vangogiel.example.finance.domain

sealed trait Event

case object AccountOpened extends Event
case object AccountClosed extends Event
