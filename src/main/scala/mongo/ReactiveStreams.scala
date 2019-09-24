package mongo

import java.util.concurrent.atomic.AtomicBoolean

import org.mongodb.{ scala => mongoDB }
import org.{ reactivestreams => rxStreams }

object ReactiveStreams {

  implicit class ObservableToPublisher[T](val inner: mongoDB.Observable[T]) extends AnyVal {

    def toPublisher: rxStreams.Publisher[T] =
      (subscriber: rxStreams.Subscriber[_ >: T]) => {
        inner.subscribe(
          new mongoDB.Observer[T]() {
            override def onSubscribe(subscription: mongoDB.Subscription): Unit =
              subscriber.onSubscribe(new rxStreams.Subscription() {
                private final val cancelled: AtomicBoolean = new AtomicBoolean

                override def request(n: Long): Unit =
                  if (!subscription.isUnsubscribed && n < 1) {
                    subscriber.onError(
                      new IllegalArgumentException(
                        "3.9 While the Subscription is not cancelled, Subscription.request(long n) " +
                          "MUST throw a java.lang.IllegalArgumentException if the argument is <= 0"
                      )
                    )
                  } else {
                    subscription.request(n)
                  }

                override def cancel(): Unit =
                  if (!cancelled.getAndSet(true)) subscription.unsubscribe()
              })

            def onNext(result: T): Unit = subscriber.onNext(result)

            def onError(e: Throwable): Unit = subscriber.onError(e)

            def onComplete(): Unit = subscriber.onComplete()
          }
        )
      }
  }

}
