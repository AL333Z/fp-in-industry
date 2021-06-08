name := "fp-in-industry"

organization := "com.al333z"

scalaVersion := "2.13.6"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val Http4sVersion = "1.0.0-M23"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-dsl"          % Http4sVersion,
  "org.http4s" %% "http4s-circe"        % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion % Test
)

libraryDependencies += "dev.profunktor" %% "fs2-rabbit" % "4.0.0"

val mongo4catsV = "0.2.9"
libraryDependencies += "io.github.kirill5k" %% "mongo4cats-core"  % mongo4catsV
libraryDependencies += "io.github.kirill5k" %% "mongo4cats-circe" % mongo4catsV

val CirceVersion = "0.14.1"
libraryDependencies += "io.circe" %% "circe-core"    % CirceVersion
libraryDependencies += "io.circe" %% "circe-generic" % CirceVersion
libraryDependencies += "io.circe" %% "circe-parser"  % CirceVersion

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.typelevel"  %% "log4cats-slf4j" % "2.1.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test

addCommandAlias("buildFmt", "all compile sbt:scalafmt scalafmt test:scalafmt")
addCommandAlias("fmt", "all sbt:scalafmt scalafmt test:scalafmt")
addCommandAlias("check", "all sbt:scalafmt::test scalafmt::test test:scalafmt::test")

scalafmtOnCompile := true
