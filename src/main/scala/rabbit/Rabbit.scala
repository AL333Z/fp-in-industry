package rabbit

import cats.data.NonEmptyList
import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource }
import dev.profunktor.fs2rabbit.config.declaration.{ DeclarationQueueConfig, Durable, NonAutoDelete, NonExclusive }
import dev.profunktor.fs2rabbit.config.{ Fs2RabbitConfig, Fs2RabbitNodeConfig }
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.model.{ AckResult, AmqpEnvelope, BasicQos, QueueName }
import fs2.Stream
import projector.event.OrderCreatedEvent

import scala.util.Try

object Rabbit {

  object Config {

    val load: IO[Fs2RabbitConfig] = IO {
      val virtualHost = "/"
      val rabbitEndpoints =
        Try(NonEmptyList.fromList(System.getenv("RABBIT_ENDPOINTS").split(",").map(_.trim).toList).get)
          .getOrElse(NonEmptyList.one("localhost"))
      val rabbitPort     = Try(System.getenv("RABBIT_PORT").toInt).getOrElse(5672)
      val rabbitUser     = Option(System.getenv("RABBIT_USERNAME"))
      val rabbitPassword = Option(System.getenv("RABBIT_PASSWORD"))
      val ssl            = Option(System.getenv("RABBIT_SSL")).exists(_.toBoolean)

      Fs2RabbitConfig(
        virtualHost = virtualHost,
        nodes = rabbitEndpoints.map(
          host =>
            Fs2RabbitNodeConfig(
              host = host,
              port = rabbitPort
          )
        ),
        username = rabbitUser,
        password = rabbitPassword,
        ssl = ssl,
        connectionTimeout = 3,
        requeueOnNack = false,
        internalQueueSize = Some(500),
        automaticRecovery = true
      )
    }
  }

  def consumerFrom(config: Fs2RabbitConfig, blocker: Blocker, decoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]])(
    implicit ce: ConcurrentEffect[IO],
    cs: ContextShift[IO]
  ): Resource[IO, (AckResult => IO[Unit], Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]])] =
    for {
      client    <- Resource.liftF[IO, Fs2Rabbit[IO]](Fs2Rabbit[IO](config, blocker))
      channel   <- client.createConnectionChannel
      queueName = QueueName("EventsFromOms")
      _ <- Resource.liftF(
            client
              .declareQueue(DeclarationQueueConfig(queueName, Durable, NonExclusive, NonAutoDelete, Map.empty))(channel)
          )
      (acker, consumer) <- Resource.liftF(
                            client.createAckerConsumer[Try[OrderCreatedEvent]](queueName, BasicQos(0, 10))(
                              channel,
                              decoder
                            )
                          )
    } yield (acker, consumer)
}
