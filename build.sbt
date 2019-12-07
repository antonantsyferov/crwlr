name := "ads-crawler"
version := "0.0.1"
organization := "com.agileengine"

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

mainClass in assembly := Some("com.agileengine.adscrawler.Application")

val akkaVersion = "2.5.26"
val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.slick" %% "slick" % "3.3.1",
  "com.h2database" % "h2" % "1.4.199",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalactic" %% "scalactic" % "3.1.0" % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.25.1" % Test
)