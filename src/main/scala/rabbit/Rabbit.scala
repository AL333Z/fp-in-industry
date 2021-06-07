package rabbit

import cats.data.NonEmptyList
import cats.effect.{ IO, Resource }
import cats.implicits._
import com.rabbitmq.client.ConnectionFactory
import dev.profunktor.fs2rabbit.config.{ Fs2RabbitConfig, Fs2RabbitNodeConfig }
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.{ AckResult, AmqpEnvelope, QueueName }
import fs2.Stream
import projector.event.OrderCreatedEvent

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
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
        nodes = rabbitEndpoints.map(
          host =>
            Fs2RabbitNodeConfig(
              host = host,
              port = rabbitPort
          )
        ),
        virtualHost = virtualHost,
        connectionTimeout = 5.seconds,
        ssl = ssl,
        username = rabbitUser,
        password = rabbitPassword,
        requeueOnNack = false,
        requeueOnReject = false,
        internalQueueSize = Some(500),
        requestedHeartbeat = FiniteDuration(ConnectionFactory.DEFAULT_HEARTBEAT, TimeUnit.SECONDS),
        automaticRecovery = true
      )
    }
  }

  def consumerFrom(
    config: Fs2RabbitConfig,
    decoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]]
  ): Resource[IO, (AckResult => IO[Unit], Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]])] =
    for {
      rabbitClient      <- RabbitClient.resource[IO](config)
      channel           <- rabbitClient.createConnectionChannel
      (acker, consumer) <- Resource.eval(rabbitClient.createAckerConsumer(QueueName("EventsFromOms"))(channel, decoder))
    } yield (acker, consumer)
}
