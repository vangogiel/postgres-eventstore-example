package io.vangogiel.example.finance

import cats.effect.Async
import cats.syntax.all.*
import io.vangogiel.example.finance.application.AccountService
import io.vangogiel.example.finance.finance_service.FinanceServiceFs2Grpc
import io.vangogiel.example.finance.infrastructure.eventstore.InMemoryEventStore
import io.vangogiel.example.finance.infrastructure.grpc.FinanceServiceAdapter
import fs2.Stream

object Server:
  def run[F[_]: Async]: Stream[F, Nothing] =
    Stream.eval(InMemoryEventStore[F].map(new AccountService[F](_))).flatMap { accountService =>
      Stream
        .resource(
          FinanceServiceFs2Grpc.bindServiceResource(FinanceServiceAdapter(accountService))
        )
        .flatMap { service =>
          GrpcServer.serve(8080, List(service))
        }
    }
