package mongo

import cats.effect.{ IO, Resource }
import cats.implicits._
import org.mongodb.scala.connection.{ ClusterSettings, SocketSettings }
import org.mongodb.scala.{ MongoClient, MongoClientSettings, MongoCredential, ServerAddress }

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.SeqHasAsJava
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

  def collectionFrom(conf: Config): Resource[IO, Collection] = {

    val addresses: List[ServerAddress] = conf.serverAddresses.map(new ServerAddress(_, conf.serverPort))
    val maybeCredential: Option[MongoCredential] = conf.auth.map(
      conf => MongoCredential.createScramSha1Credential(conf.username, "admin", conf.password.toCharArray)
    )

    val settings = MongoClientSettings
      .builder()
      .applyToClusterSettings { (t: ClusterSettings.Builder) =>
        t.hosts(addresses.asJava); ()
      }
      .applyToSocketSettings { (t: SocketSettings.Builder) =>
        t.readTimeout(30, TimeUnit.SECONDS); ()
      }

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
      .map(Collection(_))
  }
}
