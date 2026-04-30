package io.vangogiel.example.finance

import cats.effect.Async

object Server:
  def run[F[_]: Async]: F[Unit] =
    GrpcServer
      .serve(8080, List.empty)
      .compile
      .drain
