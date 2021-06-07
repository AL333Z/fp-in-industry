package projector

import cats.effect.unsafe.implicits.global
import cats.effect.{ IO, Ref }
import dev.profunktor.fs2rabbit.model._
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.log4cats.slf4j.Slf4jLogger._
import projector.event.{ OrderCreatedEvent, OrderLine }

import scala.util.{ Success, Try }

class OrderHistoryProjectorTest extends AnyFunSuite {
  test("store events") {

    val input       = OrderCreatedEvent("001", "ACME", "test@test.com", List(OrderLine(1, "item1", BigDecimal(10))))
    val deliveryTag = DeliveryTag(1)

    val consumerStub = Stream.apply[IO, AmqpEnvelope[Try[OrderCreatedEvent]]](
      AmqpEnvelope(
        deliveryTag = deliveryTag,
        payload = Success(input),
        properties = AmqpProperties.empty,
        exchangeName = ExchangeName(""),
        routingKey = RoutingKey(""),
        redelivered = false
      )
    )

    val check = for {
      repoMock  <- EventRepositoryMock()
      ackerMock <- AckerMock()
      sut = new OrderHistoryProjector(
        eventRepo = repoMock,
        consumer = consumerStub,
        acker = ackerMock.acker,
        logger = getLogger
      )
      _      <- sut.project
      stored <- repoMock.verifyInvocationFor(input)
      acked  <- ackerMock.verifyInvocationFor(deliveryTag)
    } yield stored && acked

    assert(check.unsafeRunSync())
  }

  class EventRepositoryMock private (invocations: Ref[IO, List[OrderCreatedEvent]]) extends EventRepository {
    override def store(event: OrderCreatedEvent): IO[Unit] =
      invocations.update(_ :+ event)

    def verifyInvocationFor(event: OrderCreatedEvent): IO[Boolean] = invocations.get.map(_.contains(event))
  }

  object EventRepositoryMock {
    def apply(): IO[EventRepositoryMock] = Ref.of[IO, List[OrderCreatedEvent]](List()).map(new EventRepositoryMock(_))
  }

  class AckerMock private (invocations: Ref[IO, List[DeliveryTag]]) {

    val acker: AckResult => IO[Unit] = {
      case AckResult.Ack(dt)    => invocations.update(_ :+ dt)
      case AckResult.NAck(dt)   => invocations.update(_ :+ dt)
      case AckResult.Reject(dt) => invocations.update(_ :+ dt)
    }

    def verifyInvocationFor(deliveryTag: DeliveryTag): IO[Boolean] = invocations.get.map(_.contains(deliveryTag))
  }

  object AckerMock {
    def apply(): IO[AckerMock] = Ref.of[IO, List[DeliveryTag]](List()).map(new AckerMock(_))
  }
}
