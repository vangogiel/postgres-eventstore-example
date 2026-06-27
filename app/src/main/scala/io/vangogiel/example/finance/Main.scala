package io.vangogiel.example.finance

import cats.effect.{ ExitCode, IO, IOApp }

object Main extends IOApp.Simple:
  val run: IO[Unit] = Server
    .run[IO]
    .compile
    .drain
    .as(ExitCode.Success)
