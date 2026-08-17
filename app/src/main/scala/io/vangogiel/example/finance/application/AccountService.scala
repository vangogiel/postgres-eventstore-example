package io.vangogiel.example.finance.application

import cats.Monad
import cats.syntax.all.*
import io.vangogiel.example.finance.application.port.EventStore
import io.vangogiel.example.finance.domain.AccountState.{ClosedState, OpenedState}
import io.vangogiel.example.finance.domain.{
  AccountAlreadyClosed,
  AccountNotFound,
  AccountState,
  Aggregate,
  Behaviour,
  Command,
  ConcurrentModification,
  DomainError,
  Event,
  MoneyTransferReceived,
  TransferMoney
}

class AccountService[F[_]: Monad](eventStore: EventStore[F]):

  def currentState(accountId: String): F[Option[AccountState]] =
    eventStore.readStream(accountId).map(Aggregate.rehydrate)

  def handle(accountId: String, command: Command): F[Either[DomainError, List[Event]]] =
    command match
      case TransferMoney(sourceAccountId, destinationAccountId, amount) =>
        transfer(sourceAccountId, destinationAccountId, amount)
      case _ =>
        applyToStream(accountId, Behaviour.decide(_, command))

  private def transfer(
      sourceAccountId: String,
      destinationAccountId: String,
      amount: BigDecimal
  ): F[Either[DomainError, List[Event]]] =
    val creditDestination: Option[AccountState] => Either[DomainError, List[Event]] =
      case Some(_: OpenedState) =>
        Right(List(MoneyTransferReceived(sourceAccountId, destinationAccountId, amount)))
      case Some(_: ClosedState) => Left(AccountAlreadyClosed(destinationAccountId))
      case None                 => Left(AccountNotFound(destinationAccountId))

    currentState(destinationAccountId).flatMap {
      case Some(_: ClosedState) => AccountAlreadyClosed(destinationAccountId).asLeft[List[Event]].pure[F]
      case None                 => AccountNotFound(destinationAccountId).asLeft[List[Event]].pure[F]
      case Some(_: OpenedState) =>
        applyToStream(
          sourceAccountId,
          Behaviour.decide(_, TransferMoney(sourceAccountId, destinationAccountId, amount))
        ).flatMap {
          case Left(error) => error.asLeft[List[Event]].pure[F]
          case Right(sentEvents) =>
            applyToStream(destinationAccountId, creditDestination).map(_.map(_ => sentEvents))
        }
    }

  private def applyToStream(
      accountId: String,
      decide: Option[AccountState] => Either[DomainError, List[Event]]
  ): F[Either[DomainError, List[Event]]] =
    for
      existingEvents <- eventStore.readStream(accountId)
      state = Aggregate.rehydrate(existingEvents)
      result <- decide(state) match
        case Left(error) => error.asLeft[List[Event]].pure[F]
        case Right(newEvents) =>
          eventStore.append(accountId, existingEvents.size.toLong, newEvents).map {
            case Right(())  => newEvents.asRight[DomainError]
            case Left(_)    => ConcurrentModification(accountId).asLeft[List[Event]]
          }
    yield result
