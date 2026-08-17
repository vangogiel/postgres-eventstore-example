package io.vangogiel.example.finance.infrastructure.grpc

import java.util.UUID
import cats.effect.Async
import cats.syntax.all.*
import io.grpc.{Metadata, Status}
import io.vangogiel.example.finance.account_balance_request.AccountBalanceRequest
import io.vangogiel.example.finance.account_balance_response.AccountBalanceResponse
import io.vangogiel.example.finance.application.AccountService
import io.vangogiel.example.finance.create_account_request.CreateAccountRequest
import io.vangogiel.example.finance.create_account_response.CreateAccountResponse
import io.vangogiel.example.finance.deposit_money_request.DepositMoneyRequest
import io.vangogiel.example.finance.deposit_money_response.DepositMoneyResponse
import io.vangogiel.example.finance.domain.*
import io.vangogiel.example.finance.finance_service.FinanceServiceFs2Grpc
import io.vangogiel.example.finance.transfer_money_request.TransferMoneyRequest
import io.vangogiel.example.finance.transfer_money_response.TransferMoneyResponse
import io.vangogiel.example.finance.withdraw_money_request.WithdrawMoneyRequest
import io.vangogiel.example.finance.withdraw_money_response.WithdrawMoneyResponse

object FinanceServiceAdapter:

  def apply[F[_]: Async](accountService: AccountService[F]): FinanceServiceFs2Grpc[F, Metadata] =
    new Impl(accountService)

  private[grpc] def statusFor(error: DomainError): Status = error match
    case AccountNotFound(accountId) =>
      Status.NOT_FOUND.withDescription(s"Account $accountId not found")
    case AccountAlreadyExists(accountId) =>
      Status.ALREADY_EXISTS.withDescription(s"Account $accountId already exists")
    case AccountAlreadyClosed(accountId) =>
      Status.FAILED_PRECONDITION.withDescription(s"Account $accountId is closed")
    case InsufficientFunds(accountId, balance, amount) =>
      Status.FAILED_PRECONDITION.withDescription(
        s"Account $accountId has insufficient funds: balance=$balance, requested=$amount"
      )
    case ConcurrentModification(accountId) =>
      Status.ABORTED.withDescription(s"Account $accountId was concurrently modified, please retry")

  private class Impl[F[_]: Async](accountService: AccountService[F])
      extends FinanceServiceFs2Grpc[F, Metadata]:

    private def handle[Response](accountId: String, command: Command)(
        onSuccess: List[Event] => Response
    ): F[Response] =
      accountService.handle(accountId, command).flatMap {
        case Right(events) => Async[F].pure(onSuccess(events))
        case Left(error)   => Async[F].raiseError(statusFor(error).asRuntimeException())
      }

    override def createAccount(
        request: CreateAccountRequest,
        ctx: Metadata
    ): F[CreateAccountResponse] =
      Async[F].delay(UUID.randomUUID().toString).flatMap { accountId =>
        handle(accountId, OpenAccount(accountId))(_ => CreateAccountResponse(accountId = accountId))
      }

    override def depositMoney(request: DepositMoneyRequest, ctx: Metadata): F[DepositMoneyResponse] =
      handle(request.accountId, DepositMoney(request.accountId, BigDecimal(request.amount))) { _ =>
        DepositMoneyResponse(accountId = request.accountId, amountDeposited = request.amount)
      }

    override def withdrawMoney(request: WithdrawMoneyRequest, ctx: Metadata): F[WithdrawMoneyResponse] =
      handle(request.accountId, WithdrawMoney(request.accountId, BigDecimal(request.amount))) { _ =>
        WithdrawMoneyResponse(accountId = request.accountId, amountWithdrawn = request.amount)
      }

    override def transferMoney(request: TransferMoneyRequest, ctx: Metadata): F[TransferMoneyResponse] =
      val command =
        TransferMoney(request.sourceAccountId, request.destinationAccountId, BigDecimal(request.amount))
      handle(request.sourceAccountId, command) { _ => () }.flatMap { _ =>
        accountService.currentState(request.sourceAccountId).map { state =>
          TransferMoneyResponse(
            sourceAccountId = request.sourceAccountId,
            destinationAccountId = request.destinationAccountId,
            amountTransferred = request.amount,
            sourceAccountBalance = state.map(_.balance.toLongExact).getOrElse(0L)
          )
        }
      }

    override def getAccountBalance(
        request: AccountBalanceRequest,
        ctx: Metadata
    ): F[AccountBalanceResponse] =
      accountService.currentState(request.accountId).flatMap {
        case Some(state) =>
          Async[F].pure(
            AccountBalanceResponse(accountId = request.accountId, totalBalance = state.balance.toLongExact)
          )
        case None =>
          Async[F].raiseError(statusFor(AccountNotFound(request.accountId)).asRuntimeException())
      }
