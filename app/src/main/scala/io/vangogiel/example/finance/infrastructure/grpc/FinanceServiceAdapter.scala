package io.vangogiel.example.finance.infrastructure.grpc

import java.util.UUID
import scala.util.Random
import cats.effect.Async
import io.grpc.Metadata
import io.vangogiel.example.finance.account_balance_request.AccountBalanceRequest
import io.vangogiel.example.finance.account_balance_response.AccountBalanceResponse
import io.vangogiel.example.finance.finance_service.FinanceServiceFs2Grpc
import io.vangogiel.example.finance.create_account_request.CreateAccountRequest
import io.vangogiel.example.finance.create_account_response.CreateAccountResponse
import io.vangogiel.example.finance.deposit_money_request.DepositMoneyRequest
import io.vangogiel.example.finance.deposit_money_response.DepositMoneyResponse
import io.vangogiel.example.finance.transfer_money_request.TransferMoneyRequest
import io.vangogiel.example.finance.transfer_money_response.TransferMoneyResponse
import io.vangogiel.example.finance.withdraw_money_request.WithdrawMoneyRequest
import io.vangogiel.example.finance.withdraw_money_response.WithdrawMoneyResponse

object FinanceServiceAdapter:
  def apply[F[_]: Async]: FinanceServiceFs2Grpc[F, Metadata] = new Impl

  private class Impl[F[_]: Async] extends FinanceServiceFs2Grpc[F, Metadata]:
    override def createAccount(request: CreateAccountRequest, ctx: Metadata): F[CreateAccountResponse] =
      Async[F]
        .pure(
          CreateAccountResponse(
            accountId = UUID.randomUUID().toString
          )
        )

    override def depositMoney(request: DepositMoneyRequest, ctx: Metadata): F[DepositMoneyResponse] =
      Async[F]
        .pure(
          DepositMoneyResponse(
            accountId = request.accountId,
            amountDeposited = request.amount
          )
        )

    override def withdrawMoney(request: WithdrawMoneyRequest, ctx: Metadata): F[WithdrawMoneyResponse] =
      Async[F]
        .pure(
          WithdrawMoneyResponse(
            accountId = request.accountId,
            amountWithdrawn = request.amount
          )
        )

    override def transferMoney(request: TransferMoneyRequest, ctx: Metadata): F[TransferMoneyResponse] =
      Async[F]
        .pure(
          TransferMoneyResponse(
            sourceAccountId = request.sourceAccountId,
            destinationAccountId = request.destinationAccountId,
            amountTransferred = request.amount,
            sourceAccountBalance = Random.between(request.amount, request.amount + 200) - request.amount
          )
        )

    override def getAccountBalance(request: AccountBalanceRequest, ctx: Metadata): F[AccountBalanceResponse] =
      Async[F]
        .pure(
          AccountBalanceResponse(
            accountId = request.accountId,
            totalBalance = Random.between(100, 999)
          )
        )
