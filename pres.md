autoscale: true
header: #FF6B6B
footer-style: #C44D58
text: #4ECDC4
text-emphasis: #FFFFFF
text-strong: #C7F464
header-emphasis: #C44D58
header-strong: #C44D58

## (Im)pratical Functional Programming

### _Adopting Functional Programming_
### _In industry_

---

# Who am I

## _@al333z_
### Software Engineer
### Member of _@FPinBO_ ![inline 10%](pics/fpinbo.jpg)
### Runner

![right](pics/pic.jpg)

---

# Why this talk?

### _How many times have you heard:_
- FP is _too hard_
- FP is _not pragmatic_
- FP is not suited __to deliver value to the business__

---

# Why Functional Programming?

Why not.

---

# Why Functional Programming?

- Built on __solid foundations__, but there's no need to be a mathematician to be a functional programmer!
- Offers __abstractions and techniques__ to solve concrete problems
- Improves code reuse, through __composition__
- Let us build programs which are **_simpler to reason about_**

---

# Why Developers are scared by Functional Programming?

- the *learning curve* may be steep 
- willingness to experience a _mental shift_
- may not appear _familiar_ at first
- I just don't know..

---

# Agenda

- A sample architecture
- Design architecture components
- Have some fun in the meanwhile?

---

[.background-color: #FFFFFF]

# Sample Architecture: _Order History Service_

![Inline, 70%](pics/arch.png)

- Let's assume we are provided with domain events from an Order Management Platform (e.g. OrderCreated), via a RabbitMQ broker
- We need to build an Order History Service

^ Talk ONLY about REQUIREMENTS!

---
[.background-color: #FFFFFF]

# Order History Service: _components_

![Inline, 70%](pics/arch.png)

- a component which projects a model, in a MongoDB collection
- so that an HTTP service can queries the collection returning orders

^ Talk ONLY about COMPONENTS!

---

# Disclaimer

Our focus here is **_NOT_** on the System Architecture

We'll just put our attention on _implementing an architecture component_ (the projector) using Pure Functional Programming, in Scala

---

# Why Scala

---

# Why Scala

## _I know Scala_

---

# Why Scala

- immutability, _ADTs_
- higher-kinded types + implicits -> *typeclasses*
- DSL-friendly
- discourages runtime checks (reflections, etc..)
- __mature ecosystem__ of FP libs (cats, cats-effects, fs2, circe, http4s, etc..)

---
# Let's start
---
[.background-color: #FFFFFF]

# Building a projector

![Inline, 80%](pics/projector.png)

- __Consume__ a stream of events from a RabbitMQ queue
- __Persist__ a model to a MongoDB collection

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

# Can FP help us with **I/O** operations?

---

# Introducing IO

A data type for **encoding effects** as pure values, capable of expressing both computations such that:
- can end in *either success or failure*
- on evaluation *yield exactly one result*
- may support *cancellation*
 
---

# Introducing IO

A value of type `IO[A]` is a computation that, when evaluated, can perform __effects__ before returning a value of type `A`. 

---

# IO values

- are *pure* and *immutable*
- represents just a description of a *side effectful computation*
- are not evaluated (_suspended_) until the **end of the world**

---

[.code-highlight: none]
[.code-highlight: 1-8]
[.code-highlight: 10-15]
[.code-highlight: all]

# IO and combinators

```scala
object IO {
  def delay[A](a: => A): IO[A]
  def pure[A](a: A): IO[A]
  def raiseError[A](e: Throwable): IO[A]
  def sleep(duration: FiniteDuration): IO[Unit] 
  ...
}

class IO[A] {
  def map[B](f: A => B): IO[B]
  def flatMap[B](f: A => IO[B]): IO[B]
  def *>[B](fb: IO[B]): IO[B]
  ...
}
```

---

# Composing sequential effects

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 3-5]
[.code-highlight: 3-6]
[.code-highlight: 3-7]
[.code-highlight: 3-9]
[.code-highlight: all]

```scala
val ioInt: IO[Int] = IO.delay{ println("hey!"); 1 }

val program: IO[Unit] =
 for {
    i1 <- ioInt
    _  <- IO.sleep(i.second)
    _  <- IO.raiseError(new RuntimeException("boom!")) // not throwing!
    i2 <- ioInt // not executed, comps is short-circuted
 } yield ()

> Output:
> hey
> RuntimeException: boom!
```

---

# Putting things in practice!

---

[.code-highlight: 1-3, 15]
[.code-highlight: 1, 5-7,13-15]
[.code-highlight: all]

# 1. Read a bunch of configs from the env

```scala
object Mongo {
  case class Auth(username: String, password: String)
  case class Config(auth: Option[Auth], addresses: List[String], /*...*/)
  
  object Config {
    // a delayed computation which read from env variables
    val load: IO[Config] =
      for {
        user     <- Option.delay(System.getenv("MONGO_USERNAME"))
        password <- Option.delay(System.getenv("MONGO_PASSWORD"))
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

`IOApp` is a safe application type that describes a main which executes an `IO`, as the single _entry point_ to a **pure** program.

```scala
object OrderHistoryProjectorApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load
      // TODO use configs to start the main logic!
    } yield ExitCode.Success
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
- let me introduce you a bunch of useful data types

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/lib.png)

---

# 2.1. Interact with a RabbitMQ broker
## Open a connection

```scala 
val client: Fs2Rabbit = Fs2Rabbit[IO](config)

val channel: Resource[AMQPChannel] = client.createConnectionChannel
```

### What's a `Resource`?

---

# Introducing Resource

#### Effectfully allocates and releases a resource

---

# Extremely helpful to write code that:
- doesn't leak
- handles properly terminal signals

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

```scala
val r: Resource[(String, String)] = 
  for {
    outer <- mkResource("outer")
    inner <- mkResource("inner")
  } yield (outer, inner)

r.use { case (a, b) => IO.delay(println(s"Using $a and $b")) } // IO[Unit]
```

[.code-highlight: none]
[.code-highlight: all]
```
Output:
Acquiring outer
Acquiring inner
Using outer and inner
Releasing inner
Releasing outer
```

---

# Gotchas:
- _Nested resources_ are released in *reverse order* of acquisition 
- Outer resources are released even if an inner use or release fails
- Easy to _lift_ an `AutoClosable` to `Resource`, via `Resource.fromAutoclosable`
- You can _lift_ any `IO[A]` into a `Resource[A]` with a no-op release via `Resource.liftF`

---

[.code-highlight: 4]
[.code-highlight: 6-10]
[.code-highlight: 5-12]
[.code-highlight: 13]
[.code-highlight: all]

# 2.1. Interact with a RabbitMQ broker

```scala
val client: Fs2Rabbit = Fs2Rabbit(config)

val rabbitDeps: Resource[(Acker, Consumer)] = for {
  channel <- client.createConnectionChannel // resource opening a connection to a channel
  (acker, consumer) <- Resource.liftF( // lift an IO which creates the consumer
    client.createAckerConsumer[Try[OrderCreatedEvent]](
      queueName = QueueName("EventsFromOms"),
      basicQos = BasicQos(0, 10))(
      channel = channel,
      decoder = decoder
    )
  )
} yield (acker, consumer)

type Acker = AckResult => IO[Unit]
type Consumer = Stream[AmqpEnvelope[Try[OrderCreatedEvent]]]
```

---

# I hear you...

```scala
type Consumer = 
  Stream[AmqpEnvelope[Try[OrderCreatedEvent]]]
```

---

# Introducing Stream

---

# Introducing Stream

- Describes a *sequence* of effectful computation
- **_Pull-based_**,  a consumer pulls its values by repeatedly performing one pull step at a time
- **Simplify the way we write concurrent streaming consumers**

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/stream.png)

---

# Introducing Stream

A stream _producing output_ of type `O` and which may _evaluate `IO` effects_.

```scala
class Stream[O]{
  def evalMap[O2](f: O => IO[O2]): Stream[O2]
}
```

[.footer: NB: not actual code, just a simplification sticking with IO type]

--- 

# Introducing Stream

```scala
Stream(1,2,3)
  .repeat
  .evalMap(i => IO.delay(println(i))
  .compile
  .drain
```

A sequence of effects...

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
Using the official `mongo-scala-driver`, which is *not* exposing purely functional apis..

---

# How to turn an API to be _functional<sup>TM</sup>_?

---

# How to turn an API to be _functional<sup>TM</sup>_?

In this case:
- **_wrap_** the impure type
- only **expose** the _safe_ version of its operations

---

[.code-highlight: 1,2,9]
[.code-highlight: 4-8]
[.code-highlight: all]

# "Wrap the crap"

```scala
class Collection(
  private val wrapped: MongoCollection[Document]) {

  def insertOne(document: Document): IO[Unit] =
    wrapped
      .insertOne(document)
      .toIO // <- extension method converting to IO!
      .void
}
```

---

[.code-highlight: 1,14]
[.code-highlight: 3,13]
[.code-highlight: 3-4,13]
[.code-highlight: 3,6-7,13]
[.code-highlight: 3,6-12,13]
[.code-highlight: all]

# 3.1 Open a connection

```scala
object Mongo {
  ...
 def collectionFrom(conf: Config): Resource[Collection] = {
  val clientSettings = ??? // conf to mongo-scala-driver settings
  
  Resource
   .fromAutoCloseable(IO.defer(MongoClient(clientSettings)))
   .map { client => 
           val unsafeCol = client.getDatabase(conf.databaseName)
                                 .getCollection(conf.collectionName)
           new Collection(unsafeCol)
   }
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

[.code-highlight: 1-3]
[.code-highlight: 5-6,16-17]
[.code-highlight: all]

# 3.2 Store the model to the given collection

```scala 
trait EventRepository {
  def store(event: OrderCreatedEvent): IO[Unit]
}

object EventRepository {
 def fromCollection(collection: Collection): EventRepository = new EventRepository {
   def store(event: OrderCreatedEvent): IO[Unit] =
     collection.insertOne( // using safe ops
       Document(
         "id"      -> event.id,
         "company" -> event.company,
         "email"   -> event.email,
         "lines" -> event.lines.map(line => ...)
       )
     )
  }
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

How to achieve _dependency inversion_?

---

# Wiring

- *`Reader`*/*`Kleisli`*?
- *Cake pattern*?
- *Dagger* et similia?
- Your favourite *DI framework* with xmls and reflection?

---

# Wiring

## **_Constructor Injection_**!

- **I don't like _suffering_**
- JVM application lifecycle is not so complex
- `IO`, `IOApp`, `Resource`, `Stream` are handling properly termination events

---

# Constructor Injection

### a class with a **private constructor** taking its dependencies as input

[.footer: My view of Constructor Injection for effectful applications]

---

# Constructor Injection

### a companion object 
  1. with a _`fromXXX`_ method (**smart constructor**) taking its config (or other useful params) as input
  2.  usually acquiring resources 
  3.  and returning the component as a _resource_ itself
 
[.footer: My view of Constructor Injection for effectful applications]

---

[.code-highlight: 1]
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
                  rabbitConfig: Fs2RabbitConfig
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
      collection        <- Mongo.collectionFrom(mongoConfig)
      logger            <- Resource.liftF(Slf4jLogger.create)
      (acker, consumer) <- Rabbit.consumerFrom(
                            rabbitConfig,
                            eventDecoder)
      repo = EventRepository.fromCollection(collection)
    } yield new OrderHistoryProjector(repo, consumer, acker, logger)
}
```

---

# Wiring - Constructor Injection

- **No magic at all**, each dependency is explicitly passed in the *smart constructor* of each component
- Acquiring/releasing resources is handled as an *effect*

---

[.code-highlight: 9-11]
[.code-highlight: all]

# Projector Application: Main

```scala
object OrderHistoryProjectorApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      // resolve configs from the environment
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load

      _ <- OrderHistoryProjector
            .fromConfigs(mongoConfig, rabbitConfig) // acquire the needed resources
            .use(_.project) // start to process the stream of events

    } yield ExitCode.Success
}
```

---

# All done!

![inline](pics/meme1.jpg)

---

# Things I'm not telling you

---

# Things I'm not telling you
## _Testing_

---

# Things I'm not telling you
## _Typeclasses_

---

# Things I'm not telling you
## _Higher Kinded Types_

---

# Things I'm not telling you
## _Tagless final_

---

# I've been lying to you
#### _Stream, Resource, Fs2Rabbit and HttpRoutes are polymorphic in the effect type!_

In all the slides I always omitted the additional effect type parameter!


- `Resource[F, A]`
- `Stream[F, A]`
- `Fs2Rabbit[F]`
- `HttpRoutes[F]`

#### Polymorphism is great, but comes at a (learning) cost!

---

# Conclusions

- a production-ready component in under 300 LOC
- only _3 main datatypes_: `IO`, `Resource`, `Stream`
- no _variables_, no _mutable state_
- no fancy abstractions
- no unneeded polymorphism
- __I could have written almost the same code in Kotlin, Swift or.. Haskell!__

---

# Thanks

---

# References

https://github.com/AL333Z/fp-in-industry
https://typelevel.org/cats-effect/
https://fs2.io/
https://fs2-rabbit.profunktor.dev/