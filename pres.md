autoscale: true
<!--
header: #FF6B6B
footer-style: #C44D58
text: #4ECDC4
text-emphasis: #FFFFFF
text-strong: #C7F464
header-emphasis: #C44D58
header-strong: #C44D58
-->
## (Im)practical Functional Programming

### _Adopting FP In industry_


---
<!--
# Who am I

## _@al333z_
### Software Engineer
### Member of _@FPinBO_ ![inline 10%](pics/fpinbo.jpg)
### Runner

![right](pics/pic.jpg)

---
-->

# Why this talk?

### _How many times have you heard:_
- FP is _too hard_
- FP is _not pragmatic_
- FP is not suited __to deliver value to the business__

---

# Agenda

- A sample architecture
- Introduce a bunch of building blocks
- Design architecture components

---

[.background-color: #FFFFFF]

# Sample Architecture: __Order History Service__

![Inline, 70%](pics/arch.png)

- Let's assume we are provided with domain events from an Order Management Platform (e.g. OrderCreated, OrderShipped, etc..), via a RabbitMQ broker
- We need to build an Order History Service

^ Talk ONLY about REQUIREMENTS!

---
[.background-color: #FFFFFF]

# Order History Service: __components__

![Inline, 70%](pics/arch.png)

- a component which projects a (read) model, in a MongoDB collection
- so that an HTTP service can query the collection returning orders

^ Talk ONLY about COMPONENTS!

---

# Disclaimer

Our focus here is **_NOT_** on the System Architecture

We'll just put our attention on _implementing an architecture component_ (the projector) adopting _Functional Programming_ principles

---

# Spoiler

We WON'T be using:
- `var`
- `throw`
- methods returning `Unit`
- _poorly typed definitions_ (`Any`, `Object`, etc...)
- _low level concurrency mechanisms_ (`Thread`, `Actor`, etc..)

---

<!--
# Why Scala

---

# Why Scala

## _I know Scala_

---

# Why Scala

- immutability, _ADTs_
- higher-kinded types + implicits -> *typeclasses*
- DSL-friendly
- __mature ecosystem__ of FP libs (cats, cats-effects, fs2, circe, http4s, etc..)

---
-->

# Let's start
---
[.background-color: #FFFFFF]

# Building a projector

![Inline, 80%](pics/projector.png)

- __Consume__ a stream of events from a RabbitMQ queue
- __Persist__ a read model to a MongoDB collection

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/gap.png)

---

# Projector application
1. read a bunch of configs from the env
2. interact with a RabbitMQ broker
2.1 open a connection
2.2 receive a Stream of events from the given queue
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# 1. read a bunch of configs from the env

```scala
object Mongo {
  case class Auth(username: String, password: String)
  case class Config(auth: Auth, addresses: List[String], /*...*/)
  
  // reading from env variables
  lazy val config: Config = {
      val user      = System.getenv("MONGO_USERNAME")
      val password  = System.getenv("MONGO_PASSWORD")
      //...reading other env vars ... //
      Config(Auth(user, password), endpoints, port, db, collection)
  }
}
```

---

# How to turn an API to be _functional<sup>TM</sup>_?

---

# How to turn an API to be _functional<sup>TM</sup>_?

In most cases:
> **wrap** the _impure types/operations_,
> only **expose** a _safer_ version of its operations

---

# Can FP help us with **I/O** operations?

---

# Can FP help us with **side-effects**?

---

# Introducing IO
#### A data type for **encoding effects** as pure values

---

# Introducing IO

- enable capturing and controlling actions - a.k.a _effects_ - that your program _wishes to perform_ within a _resource-safe_, _typed_ context with seamless support for _concurrency_ and _coordination_
- these effects may be _asynchronous_ (callback-driven) or _synchronous_ (directly returning values); they may _return_ within microseconds or run _infinitely_.

---

# Introducing IO

A value of type `IO[A]` is a computation that, when evaluated, can perform __effects__ before either
- yielding exactly one _result_: a value of type `A`
- raising a _failure_ (`Throwable`)

---

# IO values

- are *pure* and *immutable*
- represents just a description of a *side effectful computation*
- are not evaluated (_suspended_) until the **end of the world**
- respects _referential transparency_

---

# Referential transparency

> An expression may _be replaced_ by _its value_ (or anything having the same value) 
> _without changing the result_ of the program

---

# Why do we need Referential transparency if it works anyway?

[.column]

[.code-highlight: none]
[.code-highlight: 1-3]
[.code-highlight: 5-10]
[.code-highlight: 11-13]
[.code-highlight: all]

```scala
def askInt(): Future[Int] = 
  Future(println("Please, give me a number:"))
    .flatMap(_ => Future(io.StdIn.readLine().toInt))

def askTwoInt(): Future[(Int, Int)] =
  for {
    x <- askInt()
    y <- askInt()
  } yield (x , y)

def program(): Future[Unit] =
  askTwoInt()
    .flatMap(pair => Future(println(s"Result: ${pair}")))
```
[.column]
[.code-highlight: none]
[.code-highlight: all]
```
> Output:
> Please, give me a number:
> 4
> Please, give me a number:
> 7
> Result: (4,7)
```

---

# Why do we need Referential transparency if it works anyway?

[.column]

[.code-highlight: none]
[.code-highlight: 5-10]
[.code-highlight: all]

```scala
def askInt(): Future[Int] = 
  Future(println("Please, give me a number:"))
    .flatMap(_ => Future(io.StdIn.readLine().toInt))

def askTwoInt(): Future[(Int, Int)] =
  val sameAsk = askInt()
  for {
    x <- sameAsk
    y <- sameAsk
  } yield (x , y)

def program(): Future[Unit] =
  askTwoInt()
    .flatMap(pair => Future(println(s"Result: ${pair}")))
```
[.column]
[.code-highlight: none]
[.code-highlight: all]
```
> Output:
> Please, give me a number:
> 4
> Result: (4,4)
```

We just wanted to reduce duplication through an extract var![^1]

[^1]: Example gently stolen from my dear friend https://github.com/matteobaglini/onion-with-functional-programming

---

# Referential transparency

- code easier to _reason about_
- code easier to _refactor_
- code easier to _compose_ 
- we're already used to referential transparency since our _math_ lessons!
- we're already using a lot of data types in a referential transparent manner (e.g. `List`, `Option`, `Try`, `Either`)!

---

# How to turn an API to be _functional<sup>TM</sup>_?

In most cases:
> **wrap** the _impure types/operations_,
> only **expose** a _safer_ version of its operations
> which will need to be _referential transparent_

---

# IO and combinators

[.column]

[.code-highlight: none]
[.code-highlight: all]

```scala
object IO {
  def delay[A](a: => A): IO[A]
  def pure[A](a: A): IO[A]
  def raiseError[A](e: Throwable): IO[A]
  def sleep(duration: FiniteDuration): IO[Unit]
  def async[A](k: /* ... */): IO[A]
  ...
}
```

[.column]

[.code-highlight: none]
[.code-highlight: all]

```scala
class IO[A] {
  def map[B](f: A => B): IO[B]
  def flatMap[B](f: A => IO[B]): IO[B]
  def *>[B](fb: IO[B]): IO[B]
  ...
}
```

---

# Composing sequential effects

[.column]
[.code-highlight: 1-4]
[.code-highlight: 7-8]
[.code-highlight: 7-9]
[.code-highlight: 7-11]
[.code-highlight: 7-12]
[.code-highlight: all]

```scala
val ioInt: IO[Int] = 
  IO.delay { println("hello") }
    .map(_ => 1)

val program: IO[Unit] =
 for {
    i1 <- ioInt
    _  <- IO.sleep(i1.second)
    _  <- IO.raiseError( // not throwing!
            new RuntimeException("boom!")) 
    i2 <- ioInt //comps is short-circuted
 } yield ()
```
[.column]
[.code-highlight: none]
[.code-highlight: all]
```
> Output:
> hello
> <...1 second...>
> RuntimeException: boom!
```

---

# We are practical

---

[.code-highlight: 1-3, 15]
[.code-highlight: 1, 5-7,13-15]
[.code-highlight: 8-12]
[.code-highlight: all]

# 1. Read a bunch of configs from the env, made functional<sup>TM</sup>

```scala
object Mongo {
  case class Auth(username: String, password: String)
  case class Config(auth: Auth, addresses: List[String], /*...*/)
  
  object Config {
    // a delayed computation which read from env variables
    val load: IO[Config] =
      for {
        user     <- IO.delay(System.getenv("MONGO_USERNAME"))
        password <- IO.delay(System.getenv("MONGO_PASSWORD"))
        //...reading other env vars ... //
      } yield Config(Auth(user, password), endpoints, port, db, collection)
  }
}
```
---

# Composing effects

```scala
val ioOps = 
 for {
    mongoConfig  <- Mongo.Config.load
    rabbitConfig <- Rabbit.Config.load
    // TODO use configs to do something!
 } yield ()
```

^ Let's take a detour to talk about how this composed computation gets executed..

---
# How IO values are executed?

If IO values are just a description of _effectful computations_ which can be composed and so on... 

Who's gonna **_run_** the suspended computation then?

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/io.png)

---

[.code-highlight: 1-2,8]
[.code-highlight: all]

# *End of the world*

- `IOApp` describes a _main_ which executes an `IO`
- as the single _entry point_ to a **pure** program.

```scala
object OrderHistoryProjectorApp extends IOApp.Simple {
  override def run: IO[Unit] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load
      // TODO use configs to start the main logic!
    } yield ()
}
```
^ `IOApp` provides an **_interpreter_** which will evaluate the `IO` value returned by the `run` method, dealing with all the dirty details of the JVM runtime, so _*you don't have to*_!

---

# Projector Application
1. ~~read a bunch of configs from the env~~
2. interact with a RabbitMQ broker
2.1 open a connection
2.2 receive a Stream of events from the given queue
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# 2. Interact with a RabbitMQ broker

Using `fs2-rabbit` lib which:
- provides a _purely functional api_
- let me introduce you a bunch of useful _data types_

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/lib.png)

---

# 2.1. Interact with a RabbitMQ broker
## Open a connection

```scala 
val channel: Resource[AMQPChannel] = 
  for {
    rabbitClient <- RabbitClient.resource(config)
    channel      <- rabbitClient.createConnectionChannel
  } yield channel
```

## `Resource`?

---

# Introducing Resource

#### Effectfully allocates and releases a resource

---

# Extremely helpful to write code that:
- doesn't leak
- handles properly terminal signals (e.g. `SIGTERM`) by default (no need to register a shutdown hook)
- do _the right thing_<sup>TM</sup> by design
- avoid the need to reboot a container every once in a while :)

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/resource.png)

---

[.code-highlight: 1-5]
[.code-highlight: 7-13]
[.code-highlight: all]

# Introducing Resource

```scala
object Resource {
  def make[A](
    acquire: IO[A])(
    release: A => IO[Unit]): Resource[A]
}

class Resource[A] {
  def use[B](f: A => IO[B]): IO[B]

  def map[B](f: A => B): Resource[B]
  def flatMap[B](f: A => Resource[B]): Resource[B]
  ...
}
```

[.footer: NB: not actual code, just a simplification sticking with IO type]
^ A note on the simplification

---

[.code-highlight: 1,9]
[.code-highlight: 2-3]
[.code-highlight: 5-6]
[.code-highlight: 8]
[.code-highlight: all]

# Making a Resource

```scala
def mkResource(s: String): Resource[String] = {
  val acquire = 
    IO.delay(println(s"Acquiring $s")) *> IO.pure(s)

  def release(s: String) = 
    IO.delay(println(s"Releasing $s"))

  Resource.make(acquire)(release)
}
```
---

[.code-highlight: 1-3]
[.code-highlight: 1-4]
[.code-highlight: all]

# Using a Resource

[.column]

```scala
val r: Resource[(String, String)] = 
  for {
    outer <- mkResource("outer")
    inner <- mkResource("inner")
  } yield (outer, inner)

r.use { case (a, b) => 
  IO.delay(println(s"Using $a and $b")) 
} // IO[Unit]
```

[.column]

[.code-highlight: none]
[.code-highlight: all]
```
Output:
> Acquiring outer
> Acquiring inner
> Using outer and inner
> Releasing inner
> Releasing outer
```

---

# Using a Resource

[.column]

```scala
val sessionPool: Resource[MySessionPool] = 
  for {
    connection <- openConnection()
    sessions   <- openSessionPool(connection)
  } yield sessions

sessionPool.use { sessions =>
  // use sessions to do whatever things!
}
```

[.column]

[.code-highlight: none]
[.code-highlight: all]

```
Output:
> Acquiring connection
> Acquiring sessions
> Using sessions
> Releasing sessions
> Releasing connection
```

---

# Gotchas:
- _Nested resources_ are released in *reverse order* of acquisition 
- Easy to _lift_ an `AutoClosable` to `Resource`, via `Resource.fromAutoclosable`
- Every time you need to use something which implements `AutoClosable`, you should really be using `Resource`!
- You can _lift_ any `IO[A]` into a `Resource[A]` with a no-op release via `Resource.eval`

---

# Why not scala.util.Using?

- not composable (no `map`, `flatMap`, etc...)
- no support for properly handling effects

---

# We are pragmatic

---

[.code-highlight: 8]
[.code-highlight: 9]
[.code-highlight: 11-15]
[.code-highlight: 10-16]
[.code-highlight: 17]
[.code-highlight: all]

# 2.1. Interact with a RabbitMQ broker

```scala
object Rabbit {
  //...
  def consumerFrom(
      config: Fs2RabbitConfig,
      decoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]]
    ): Resource[(Acker, Consumer)] = 
      for {
        rabbitClient <- RabbitClient.resource(config)
        channel      <- rabbitClient.createConnectionChannel
        (acker, consumer) <- Resource.eval(
                              rabbitClient.createAckerConsumer(
                                queueName = QueueName("EventsFromOms"),
                                channel = channel,
                                decoder = decoder
                              ) // IO[(Acker, Consumer)]
                            )
      } yield (acker, consumer)
  
  type Acker    = AckResult => IO[Unit]
  type Consumer = Stream[AmqpEnvelope[Try[OrderCreatedEvent]]]
}
```

---

# I hear you...

```scala
type Consumer = 
  Stream[AmqpEnvelope[Try[OrderCreatedEvent]]]
```

---

# Introducing Stream
#### A *sequence* of effectful computation

---

# Introducing Stream

- **Simplify the way we write concurrent streaming consumers**
- **_Pull-based_**, a consumer pulls its values by repeatedly performing pull steps

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/stream.png)

---

[.code-highlight: 1-6]
[.code-highlight: 8-13]
[.code-highlight: all]

# Introducing Stream

A stream _producing output_ of type `O` and which may _evaluate `IO` effects_.

```scala
object Stream {
  def emit[A](a: A): Stream[A]
  def emits[A](as: List[A]): Stream[A]
  def eval[A](f: IO[A]): Stream[A]
  ...
}

class Stream[O]{
  def evalMap[O2](f: O => IO[O2]): Stream[O2]
  ...
  def map[O2](f: O => O2): Stream[O2]
  def flatMap[O2](f: O => Stream[O2]): Stream[O2]
}
```

[.footer: NB: not actual code, just a simplification sticking with IO type]

--- 

# Introducing Stream

A sequence of effects...

```scala
Stream(1,2,3)
  .repeat
  .evalMap(i => IO.delay(println(i))
  .compile
  .drain
```

---

# We deliver

---

[.code-highlight: 1,14]
[.code-highlight: 2]
[.code-highlight: 3,12]
[.code-highlight: 4-11]
[.code-highlight: all]

# Consuming a Stream

```scala 
class OrderHistoryProjector(consumer: Consumer, acker: Acker, logger: Logger) {
  val project: IO[Unit] =
    consumer.evalMap { envelope =>
      envelope.payload match {
        case Success(event) =>
          logger.info("Received: " + envelope) *>
            acker(AckResult.Ack(envelope.deliveryTag))
        case Failure(e) =>
          logger.error(e)("Error while decoding") *>
            acker(AckResult.NAck(envelope.deliveryTag))
      }
    }
     .compile.drain
}
```

---

# Projector application
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# 3. Interact with a MongoDB cluster
Using `mongo4cats`, a thin wrapper over the official mongodb driver, which is exposes purely functional apis

---

[.code-highlight: 1,12]
[.code-highlight: 3,11]
[.code-highlight: 3-4,11]
[.code-highlight: 3-7,11]
[.code-highlight: 3-9,11]
[.code-highlight: 3-10,11]
[.code-highlight: all]

# 3.1 Open a connection

```scala
object Mongo {
  ...
  def collectionFrom(conf: Config): Resource[MongoCollection[Order]] = {
    val clientSettings = ??? // conf to mongo-scala-driver settings

    for {
      client     <- MongoClient.create(settings.build())
      db         <- Resource.eval(client.getDatabase(conf.databaseName))
      collection <- Resource.eval(db.getCollectionWithCirceCodecs[Order](conf.collectionName))
    } yield collection
  }
}
```

---

# Projector application
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. interact with a MongoDB cluster
3.1 ~~open a connection~~
3.2 store the model to the given collection

---

[.code-highlight: 1, 12]
[.code-highlight: 2]
[.code-highlight: 3-11]
[.code-highlight: all]

# 3.2 Store the model to the given collection

```scala 
class EventRepository(collection: MongoCollection[Order]) {
  def store(event: OrderCreatedEvent): IO[Unit] =
    collection
      .insertOne(
        Order(
          orderNo = OrderNo(event.id),
          company = Company(event.company),
          email = Email(event.email),
          lines = event.lines.map(...)
        )
      )
}
```

---

[.code-highlight: 1]
[.code-highlight: 10]
[.code-highlight: all]

# 3.2 Store the model to the given collection

```scala
class OrderHistoryProjector(eventRepo: EventRepository,  
                            consumer: Consumer,  
                            acker: Acker,  
                            logger: Logger) {
  val project: IO[Unit] =
    consumer.evalMap { envelope =>
      envelope.payload match {
        case Success(event) =>
          logger.info("Received: " + envelope) *>
            eventRepo.store(event) *>
              acker(AckResult.Ack(envelope.deliveryTag))
        case Failure(e) =>
          logger.error(e)("Error while decoding") *>
            acker(AckResult.NAck(envelope.deliveryTag))
      }
    }
     .compile.drain 
}
```

---

# Projector application
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. ~~interact with a MongoDB cluster~~
3.1 ~~open a connection~~
3.2 ~~store the model to the given collection~~

---

# Wiring

How to achieve _separation of concerns_ and have a good _modularity_?

---

# Wiring

## **_Constructor Injection_**!

- JVM application lifecycle is not so complex
- **No need for magic**, each dependency can be explicitly injected
- Acquiring/releasing resources should be handled as an *effect*

---

## Introducing Constructor Injection
#### How __not to suffer__ while injecting dependencies

---

# Constructor Injection

- a class with a **private constructor**
- a companion object with a _`fromX/make`_ method (**smart constructor**)
  1. taking dependencies as input
  2. usually returning `IO`/`Resource` of the component class
 
[.footer: My view of Constructor Injection for effectful applications]

---

[.code-highlight: 1]
[.code-highlight: 1-5]
[.code-highlight: 9-13]
[.code-highlight: all]

# Wiring - Constructor Injection

```scala
class OrderHistoryProjector private (
  eventRepo: EventRepository, 
  consumer: Consumer, 
  acker: Acker, 
  logger: Logger) {
  ...
}

object OrderHistoryProjector {
  def fromConfigs(mongoConfig: Mongo.Config,
                  rabbitConfig: Rabbit.Config
  ): Resource[OrderHistoryProjector] = ...
}
```

---

# Wiring - Constructor Injection

```scala
object OrderHistoryProjector {
  def fromConfigs(
    mongoConfig: Mongo.Config,
    rabbitConfig: Fs2RabbitConfig
  ): Resource[OrderHistoryProjector] =
    for {
      (acker, consumer) <- Rabbit.consumerFrom(rabbitConfig, eventDecoder)
      collection        <- Mongo.collectionFrom(mongoConfig)
      repo               = EventRepository.fromCollection(collection)
      logger            <- Resource.eval(Slf4jLogger.create)
    } yield new OrderHistoryProjector(repo, consumer, acker, logger)
}
```

---

[.code-highlight: 8-10]
[.code-highlight: all]

# Wiring - Main

```scala
object OrderHistoryProjectorApp extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load

      _ <- OrderHistoryProjector
            .fromConfigs(mongoConfig, rabbitConfig) // acquire the needed resources
            .use(_.project) // start to process the stream of events

    } yield ()
}
```

---

# All done!

![inline](pics/meme1.jpg)

---

# There's a lot more to talk about

- How to handle concurrency, execution contexts, blocking ops?
- How to track and handle errors?
- Do we really need advanced techniques?

---

# Conclusions

- a production-ready component in under 300 LOC
- only _3 main datatypes_: `IO`, `Resource`, `Stream`
- no _variables_, no _mutable state_
- not even the M word!
- __I could have written almost the same code in Kotlin, Swift or.. Haskell!__

---

# References

https://github.com/AL333Z/fp-in-industry
https://typelevel.org/cats-effect/
https://fs2.io/
https://fs2-rabbit.profunktor.dev/

---

# Thanks

---

# I've been lying to you
#### _Stream, Resource, RabbitClient and MongoCollection are polymorphic in the effect type!_

In all the slides I always omitted the additional effect type parameter!


- `Resource[F, A]`
- `Stream[F, A]`
- `RabbitClient[F]`
- `MongoCollection[F]`

#### Polymorphism is great, but comes at a (learning) cost!
