package io.vangogiel.example.finance

import cats.effect.{ Async, Resource }
import cats.implicits.*
import fs2.Stream
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ Server, ServerServiceDefinition }

object GrpcServer:

  def serve[F[_]: Async](
      port: Int,
      services: List[ServerServiceDefinition]
  ): Stream[F, Nothing] =
    Stream.resource(resource(port, services)) >>
      Stream.never

  private def resource[F[_]: Async](
      port: Int,
      services: List[ServerServiceDefinition]
  ): Resource[F, Server] =
    Resource.make {
      Async[F].delay {
        val builder = NettyServerBuilder.forPort(port)
        services.foreach(builder.addService)
        builder.addService(ProtoReflectionService.newInstance())
        builder.build()
      }.flatTap(server => Async[F].delay(server.start()))
    } { server =>
      Async[F].delay(server.shutdown()).void
    }
