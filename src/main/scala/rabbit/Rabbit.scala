package rabbit

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, IO, Resource }
import dev.profunktor.fs2rabbit.algebra.ConnectionResource
import dev.profunktor.fs2rabbit.config.{ Fs2RabbitConfig, Fs2RabbitNodeConfig }
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.interpreter.LiveInternalQueue
import dev.profunktor.fs2rabbit.model.{ AckResult, AmqpEnvelope, BasicQos, QueueName }
import dev.profunktor.fs2rabbit.program.AckConsumingProgram
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

  def consumerFrom(config: Fs2RabbitConfig, decoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]])(
    implicit ce: ConcurrentEffect[IO]
  ): Resource[IO, (AckResult => IO[Unit], Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]])] =
    for {
      connEff          <- Resource.liftF(ConnectionResource.make[IO](config))
      internalQ        = new LiveInternalQueue[IO](config.internalQueueSize.getOrElse(500))
      ackConsumingProg <- Resource.liftF(AckConsumingProgram.make[IO](config, internalQ))
      channel          <- connEff.createConnection.flatMap(connEff.createChannel)
      (acker, consumer) <- Resource.liftF(
                            ackConsumingProg.createAckerConsumer[Try[OrderCreatedEvent]](
                              channel,
                              QueueName("EventsFromOms"),
                              BasicQos(0, 10)
                            )(decoder)
                          )
    } yield (acker, consumer)
}
