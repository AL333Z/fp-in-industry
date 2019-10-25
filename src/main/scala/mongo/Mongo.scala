package mongo

import java.util.concurrent.TimeUnit

import cats.effect.{ ConcurrentEffect, IO, Resource }
import cats.implicits._
import org.mongodb.scala.connection.{ ClusterSettings, SocketSettings }
import org.mongodb.scala.{
  MongoClient,
  MongoClientSettings,
  MongoCredential,
  Observer,
  ServerAddress,
  SingleObservable
}

import scala.collection.JavaConverters._
import scala.util.Try

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

    val load: IO[Config] = IO {
      val db         = "thor"
      val collection = "order_history"
      val mongoEndpoints =
        Try(System.getenv("MONGO_ENDPOINTS").split(",").map(_.trim).toList).getOrElse(List("localhost"))
      val mongoPort     = Try(System.getenv("MONGO_PORT").toInt).getOrElse(27017)
      val mongoUser     = Option(System.getenv("MONGO_USERNAME"))
      val mongoPassword = Option(System.getenv("MONGO_PASSWORD"))
      val auth          = (mongoUser, mongoPassword).mapN(Auth)
      Config(auth, mongoEndpoints, mongoPort, db, collection)
    }
  }

  def collectionFrom(conf: Config)(implicit ce: ConcurrentEffect[IO]): Resource[IO, Collection] = {

    val addresses: List[ServerAddress] = conf.serverAddresses.map(new ServerAddress(_, conf.serverPort))
    val maybeCredential: Option[MongoCredential] = conf.auth.map(
      conf => MongoCredential.createScramSha1Credential(conf.username, "admin", conf.password.toCharArray)
    )

    val settings = MongoClientSettings.builder
      .applyToClusterSettings((t: ClusterSettings.Builder) => t.hosts(addresses.asJava))
      .applyToSocketSettings((t: SocketSettings.Builder) => t.readTimeout(30, TimeUnit.SECONDS))

    Resource
      .fromAutoCloseable(
        IO {
          maybeCredential match {
            case Some(credentials) => MongoClient(settings.credential(credentials).build())
            case None              => MongoClient(settings.build())
          }
        }
      )
      .map(_.getDatabase(conf.databaseName).getCollection(conf.collectionName))
      .map(new Collection(_))
  }

  implicit class SingleObservableToIO[A](val inner: SingleObservable[A]) extends AnyVal {

    def toIO: IO[A] =
      IO.async(
        k =>
          inner
            .subscribe(new Observer[A] {
              override def onNext(result: A): Unit     = k(result.asRight[Throwable])
              override def onError(e: Throwable): Unit = k(e.asLeft)
              override def onComplete(): Unit          = ()
            })
      )
  }
}
