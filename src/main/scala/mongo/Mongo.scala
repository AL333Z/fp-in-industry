package mongo

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.implicits._
import org.mongodb.scala.connection.{ ClusterSettings, SocketSettings }
import org.mongodb.scala.{ Document, MongoClient, MongoClientSettings, MongoCollection, MongoCredential, ServerAddress }

import scala.collection.JavaConverters._

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

    val load = IO {
      val db         = "thor"
      val collection = "order_history"
      val mongoEndpoints =
        Option(System.getenv("MONGO_ENDPOINTS").split(",").map(_.trim).toList).getOrElse(List("localhost"))
      val mongoPort     = System.getenv("MONGO_PORT").toInt
      val mongoUser     = Option(System.getenv("MONGO_USERNAME"))
      val mongoPassword = Option(System.getenv("MONGO_PASSWORD"))
      val auth          = (mongoUser, mongoPassword).mapN(Auth)

      Mongo.Config(auth, mongoEndpoints, mongoPort, db, collection)
    }
  }

  def collectionFrom(conf: Config): IO[MongoCollection[Document]] = IO {

    val addresses: List[ServerAddress] = conf.serverAddresses.map(new ServerAddress(_, conf.serverPort))
    val maybeCredential: Option[MongoCredential] = conf.auth.map(
      conf =>
        MongoCredential.createScramSha1Credential(
          conf.username,
          "admin",
          conf.password.toCharArray
      )
    )

    val settings = MongoClientSettings.builder
      .applyToClusterSettings((t: ClusterSettings.Builder) => t.hosts(addresses.asJava))
      .applyToSocketSettings((t: SocketSettings.Builder) => t.readTimeout(30, TimeUnit.SECONDS))

    val mongoClient = maybeCredential match {
      case Some(credentials) => MongoClient(settings.credential(credentials).build())
      case None              => MongoClient(settings.build())
    }

    mongoClient.getDatabase(conf.databaseName).getCollection(conf.collectionName)
  }

}
