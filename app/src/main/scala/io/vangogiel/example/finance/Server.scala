package io.vangogiel.example.finance

import cats.effect.Async
import io.vangogiel.example.finance.finance_service.FinanceServiceFs2Grpc
import io.vangogiel.example.finance.infrastructure.grpc.FinanceServiceAdapter
import fs2.Stream

object Server:
  def run[F[_]: Async]: Stream[F, Nothing] =
    Stream.resource(
      FinanceServiceFs2Grpc.bindServiceResource(FinanceServiceAdapter.apply())
    ).flatMap { service =>
      GrpcServer.serve(8080, List(service))
    }
