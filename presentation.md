<!-- $theme: default -->

# Why this talk?

How many times have you heard/said:
- FP is too hard
- FP is not pratical
- FP is not suited to deliver value to the business 

---
# Why Functional Programming?

Why not.

---
# Why Functional Programming?

- Built on solid foundations, but there's no need to be a mathematician to be a functional programmer!
- Offers **abstractions and tecniques** to solve concrete problems
- Improves code reuse, through **composition**
- Let us build programs which are **simpler to reason about**

---
# Why Programmers are scared by Functional Programming?

- The learning curve may be steep, depending on 
  - previous experiences
  - willingness to experience a mental shift
- May not appear familiar at first, you just need to stick to it for a while
- *Inertia*
- I don't know.. are you willing to help me with point? :)

---
# Agenda

- A sample architecture
- Design architecture components using FP tecninques and abstractions
- Have some fun in the meanwhile?

---
# Sample Architecture: Order History Service

```text
+-----------------+         +---------------+
| Order Managment |         | Order History |
|    System       +-------->+   Projector   |
+-----------------+ domain  +-------+-------+
                    events          |          +----------------+
                                    |          | Order History  |
                                    |          |     API        |
                                    v          +-----+----------+
                            +-------+-------+        |
                            | Order History |  query |
                            |   Projection  +<-------+
                            +---------------+  
```

Let's assume we are provided with domain events from an Order Managment System (e.g. OrderCreated), via a RabbitMQ broker. 
In this session we'll build:
- a component which is projecting a read model, in a MongoDB collection
- a simple HTTP service, querying the collection to implement an Order History Service

---
# Disclaimer

Our focus here is NOT on the System Architecture

We'll just put our attention on **implementing architecture components** using Pure Functional Programming, in Scala

---
# Why Scala
---
# Why Scala
- I know Scala
---
# Why Scala

- first-class support for many language features needed to implement functional abstractions
  - immutable data types, ADTs
  - higher-kinded types + implicits -> typeclasses
  - DSL-friendly
  - discourage runtime checks (reflections, etc..)
- mature ecosystem of FP libs (cats, cats-effects, fs2, circe, http4s, etc..)

---
# Let's start
---

# Building a projector

```text
         +---------------+
         | Order History |
-------->+   Projector   |
 domain  +-------+-------+
 events          |        
                 |        
                 |        
                 v        
         +-------+-------+
         | Order History |
         |   Projection  |
         +---------------+
```

- **Consume** a *stream of events* from a RabbitMQ queue
- **Persist** a model to a MongoDB collection

---

# The plan
The projector application should:
1. read a bunch of configs from the env
2. interact with a RabbitMQ broker
2.1 open a connection
2.2 receive a Stream of events from the given queue
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# Can FP help us with I/O operations?

---
# Introducing IO

A data type for **encoding side effects** as pure values, capable of expressing both computations such that:
- on evaluation *yield* exactly one result
- can end in *either success or failure* (and in case of a failure the resulting composed computations gets short-circuited)
- may support *cancellation*
 

A value of type `IO[A]` is a computation which, when evaluated, can perform effects before returning a value of type A. 

---

# IO values

- are *pure* and *immutable*
- represents just a description of a *side effectful computation*
- are **not evaluated** until the *"end of the world"*

---

# IO and combinators

```scala
object IO {
  def delay[A](a: => A): IO[A]
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): IO[A]
  def pure[A](a: A): IO[A]
  def raiseError[A](e: Throwable): IO[A]
  def sleep(duration: FiniteDuration): IO[Unit] 
  ...
}

class IO[+A] {
  def map[B](f: A => B): IO[B]
  def flatMap[B](f: A => IO[B]): IO[B]
  def *>[B](fb: IO[B]): IO[B]
  ...
}
```
---

# Composing sequential effects

```scala
val ioa = IO.delay{ println("hey!") }

val program: IO[Unit] =
 for {
    _ <- ioa
    _ <- IO.sleep(1.second)
    _ <- IO.raiseError(new RuntimeException("boom"))
    _ <- ioa // not executed, comps is short-circuted
 } yield ()
```
---

# A first pratical sample
## 1. Read a bunch of configs from the env

```scala
object Mongo {
  case class Auth(username: String, password: String)
  case class Config(
    auth: Option[Auth],
    serverAddresses: List[String],
    serverPort: Int,
    databaseName: String,
    collectionName: String
  )
  
  object Config {
    val load: IO[Config] = IO.delay {      
      val user     = Option(System.getenv("MONGO_USERNAME"))
      val password = Option(System.getenv("MONGO_PASSWORD"))
      val auth = (user, password).mapN(Auth)
      //...
      Config(auth, endpoints, port, db, collection)
    }
  }
```
---

# How IO values are executed?

If IO values are just a description of a side effectful computation which can be composed and so on... 

Who's gonna **run** the composed computation then?

```scala
val ioOps = 
 for {
    mongoConfig  <- Mongo.Config.load
    rabbitConfig <- Rabbit.Config.load
    // TODO use configs to do something!
 } yield ()
```

---

# *End of the world*

`IOApp` is a safe application type that describes a main which executes an `IO`, as the single **entry point** to a pure FP program.

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

`IOApp` provides an **interpreter** which will evaluate the `IO` value returned by the `run` method, dealing with all the dirty details of the JVM runtime, so you don't have to!

---

# The plan
The projector application should:
1. ~~read a bunch of configs from the env~~
2. interact with a RabbitMQ broker
2.1 open a connection
2.2 receive a Stream of events from the given queue
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# 2. Interact with a RabbitMQ broker

Not gonna reinvent the whell, just using `fs2-rabbit` lib which:
- provides a purely functional api
- let me introduce you a bunch of useful data types

---

# 2.1. Interact with a RabbitMQ broker
## Open a connection

```scala 
val client = Fs2Rabbit[IO](config)
// Fs2Rabbit[IO]
val channel = client.createConnectionChannel
//  Resource[IO, AMQPChannel]
```

### What's a `Resource`?

---

# Introducing Resource

---

# Introducing Resource

- Effectfully allocates and releases a resource
- Generic in its effect type `F[_]`

```scala
class Resource[F[_], A] {
  def use[B](f: A => F[B]): F[B]
  def map[A, B](f: A => B): Resource[F, B]
  def flatMap[A, B](f: A => Resource[F, B]): Resource[F, B]
  ...
}

object Resource {
  def make[F[_], A](
    acquire: F[A])(
    release: A => F[Unit]): Resource[F, A]
}
```

Extremely helpfull to write code that:
- doesn't leak
- handles properly terminal signals

---

# Introducing Resource - simplified

- Sticking to `IO`...

```scala
class Resource[A] {
  def use[B](f: A => IO[B]): IO[B]
  ...
}

object Resource {
  def make[A](
    acquire: IO[A])(
    release: A => IO[Unit]): Resource[A]
}
```

---

# Making a Resource

```scala
def mkResource(s: String): Resource[IO, String] = {
  val acquire = 
    IO(println(s"Acquiring $s")) *> IO.pure(s)

  def release(s: String) = 
    IO(println(s"Releasing $s"))

  Resource.make(acquire)(release)
}
```
---

# Using a Resource

```
val r = for {
  outer <- mkResource("outer")
  inner <- mkResource("inner")
} yield (outer, inner)
// Resource[IO, (String, String)]

r.use { case (a, b) => IO(println(s"Using $a and $b")) }
// IO[Unit]
```

```
Acquiring outer
Acquiring inner
Using outer and inner
Releasing inner
Releasing outer
```

---

# Gotchas:
- Nested resources are released in reverse order of acquisition 
- Outer resources are released even if an inner use or release fails
- Easy to lift an `AutoClosable` to `Resource`, via `Resource.fromAutoclosable`
- You can lift any `F[A]` (`IO[A]`) into a `Resource[F, A]` (`Resource[IO, A]`) with a no-op release via `Resource.liftF`

---

# 2.1. Interact with a RabbitMQ broker

```scala 
    type Acker = AckResult => IO[Unit] 
    type Consumer = Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]]
    
    val client: Fs2Rabbit[IO] = Fs2Rabbit[IO](config)
    
    val rabbitDeps = for {
      channel <- client.createConnectionChannel
      (acker, consumer) <- Resource.liftF(
        client.createAckerConsumer[Try[OrderCreatedEvent]](
          queueName = QueueName("EventsFromOms"),
          basicQos = BasicQos(0, 10))(
          channel = channel,
          decoder = decoder
        )
      )
    } yield (acker, consumer)
    // Resource[IO, (Acker, Consumer)]
```

---

# I hear you..

```scala
type Consumer = 
  Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]]
```

---

# Introducing Stream

---

# Introducing Stream

A stream producing output of type `O` and which may evaluate `F` effects.

```scala
class Stream[F[_], +O]{
  def evalMap[O2](f: O => F[O2]): Stream[F, O2]
}
```

- Describes an effectful computation, just like `IO`
- Pull-based,  a consumer pulls its values by repeatedly performing one pull step at a time
- Simplify the way we write concurrent streaming consumers

---

# Introducing Stream - simplified

A stream producing output of type `O` and which may evaluate `IO` effects.

```scala
class Stream[+O]{
  def evalMap[O2](f: O => IO[O2]): Stream[IO, O2]
}
```

--- 

// TODO something more on Stream

---


# Let's wrap up

```scala 
class OrderHistoryProjector (
  consumer: Consumer,
  acker: Acker,
  logger: Logger
) {
 val project: IO[Unit] =
  (for {
    _ <- consumer.evalMap { envelope =>
     for {
       _ <- envelope.payload match {
          case Success(event) =>
            logger.info("Received: " + event) *>
              acker(AckResult.Ack(envelope.deliveryTag))
          case Failure(e) =>
            logger.error(e)("Error while decoding") *>
              acker(AckResult.NAck(envelope.deliveryTag))
       }
     } yield ()
   }
  } yield ()).compile.drain
}
```

---

# The plan
The projector application should:
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. interact with a MongoDB cluster
3.1 open a connection
3.2 store the model to the given collection

---

# 3. Interact with a MongoDB cluster
Using the official `mongo-scala-driver`, which is not exposing purely functional apis..

---

# How to turn an API to be _functional_?

---

# How to turn an API to be _functional_?

In this case:
- **wrap** the impure type
- only **expose** the _safe_ version of its operations

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

# 3.1 Open a connection

```scala
object Mongo {
  ...

 def collectionFrom(conf: Config): Resource[IO, Collection] = {
  val settings = ??? // conf to mongo-scala-driver settings
  
  Resource
   .fromAutoCloseable(
    IO.defer {
       MongoClient(
        settings.credential(conf.credentials).build())
    }
   )
    .map(_.getDatabase(conf.databaseName)
          .getCollection(conf.collectionName))
    .map(new Collection(_)) // create our safe wrapper!
 }
}
```
---

# The plan
The projector application should:
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. interact with a MongoDB cluster
3.1 ~~open a connection~~
3.2 store the model to the given collection

---

# 3.2 Store the model to the given collection

```scala 
trait EventRepository {
  def store(event: OrderCreatedEvent): IO[Unit]
}

object EventRepository {

 def fromCollection(collection: Collection): EventRepository =
  new EventRepository {
   def store(event: OrderCreatedEvent): IO[Unit] =
    collection.insertOne(
     Document(
       "id"      -> event.id,
       "company" -> event.company,
       "email"   -> event.email,
       "lines" -> event.lines.map(
         line => ...
       )
     )
    )
  }
}
```
---

# 3.2 Store the model to the given collection

```scala
class OrderHistoryProjector (
  eventRepo: EventRepository,
  consumer: Consumer,
  acker: Acker,
  logger: Logger
) {
 val project: IO[Unit] =
  (for {
   _ <- consumer.evalMap { envelope =>
         for {
          _ <- envelope.payload match {
               case Success(event) =>
                 logger.info("Received: " + envelope) *> 
                  eventRepo.store(event) *>
                   acker(AckResult.Ack(envelope.deliveryTag))
               case Failure(e) =>
                 logger.error(e)("Error while decoding") *>
                   acker(AckResult.NAck(envelope.deliveryTag))
               }
         } yield ()
       }
  } yield ()).compile.drain
}
```

---

# The plan
The projector application should:
1. ~~read a bunch of configs from the env~~
2. ~~interact with a RabbitMQ broker~~
2.1 ~~open a connection~~
2.2 ~~receive a Stream of events from the given queue~~
3. ~~interact with a MongoDB cluster~~
3.1 ~~open a connection~~
3.2 ~~store the model to the given collection~~

---

# Wiring

How to achieve dependency inversion?

---

# Wiring

- Reader/Kleisli?
- Cake pattern?
- Dagger et similia?
- Your favourite DI framework with xmls and reflection?

---

# Wiring

## Constructor Injection!

- I hate all the others (and yes, I tried all of them)
- JVM application lifecycle is not so complex
- `IO`, `SafeApp`, `Resource` are hanlding properly termination events
 
---

# Constructor Injection

```scala
class OrderHistoryProjector private (
  eventRepo: EventRepository,
  consumer: Consumer,
  acker: Acker,
  logger: Logger
) {
  ...
}

object OrderHistoryProjector {
  def fromConfigs(mongoConfig: Mongo.Config,
                  rabbitConfig: Fs2RabbitConfig
  ): Resource[IO, OrderHistoryProjector] = ...
}
```

- a class with a **private constructor** taking its deps as input
- a companion object 
  - with a `fromXXX` method (**smart constructor**) taking its config (or other useful params) as input
  -  usually acquiring resources 
  -  and returning the component as a resource itself

---

# Wiring - Constructor Injection

```scala
object OrderHistoryProjector {
  def fromConfigs(
    mongoConfig: Mongo.Config,
    rabbitConfig: Fs2RabbitConfig
  ): Resource[IO, OrderHistoryProjector] =
    for {
      collection <- Mongo.collectionFrom(mongoConfig)
      logger     <- Resource.liftF(Slf4jLogger.create[IO])
      (acker, consumer) <- Rabbit.consumerFrom(
                            rabbitConfig,
                            eventDecoder)
      repo = EventRepository.fromCollection(collection)
    } yield 
       new OrderHistoryProjector(repo, consumer, acker, logger)
}
```

- **No magic at all**, each dependency is explicitely passed in the *smart constructor* of each component.
- Acquiring/releasing resources is handled as an *effect*

---

# OrderHistoryProjectorApp - Main

```scala
object OrderHistoryProjectorApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load
      _ <- OrderHistoryProjector
            .fromConfigs(mongoConfig, rabbitConfig)
            .use(_.project)
    } yield ExitCode.Success
}
```

- resolve configs from the environment
- acquire the needed resources
- start to process the stream of events

---
# Sample Architecture: Order History Service

```text
+-----------------+         +---------------+
| Order Managment |         | Order History |
|    System       +-------->+   Projector   |
+-----------------+ domain  +-------+-------+
                    events          |          +----------------+
                                    |          | Order History  |
                                    |          |     API        |
                                    v          +-----+----------+
                            +-------+-------+        |
                            | Order History |  query |
                            |   Projection  +<-------+
                            +---------------+  
```
---

# Let's move to the other half..

---

# Building an HTTP api

```text
                   +----------------+
                   | Order History  |
                   |     API        |
                   +-----+----------+
+-------+-------+        |
| Order History |  query |
|   Projection  +<-------+
+---------------+  
```

---

# You already know of to handle effects!

---


---