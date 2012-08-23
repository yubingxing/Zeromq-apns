name := "Zeromq-apns"

version := "1.0"

scalaVersion := "2.9.2"

organization := "com.icestar"

resolvers ++= Seq(
	Classpaths.typesafeResolver,
	"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
	"Typesafe Repository" at "http://repo.typesafe.com/typesafe/release/"
)

libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-classic" % "1.0.3" % "runtime" withSources(),
	"commons-pool" % "commons-pool" % "1.6",
	"com.typesafe.akka" % "akka-slf4j" % "2.0.3" withSources(),
	"com.typesafe.akka" % "akka-kernel" % "2.0.3" withSources(),
	"com.typesafe.akka" % "akka-actor" % "2.0.3" withSources(),
	"com.typesafe.akka" % "akka-zeromq" % "2.0.3" withSources(),
	"com.typesafe.akka" % "akka-remote" % "2.0.3" withSources(),
	"com.typesafe.akka" % "akka-testkit" % "2.0.3" % "test",
	"org.scalatest" %% "scalatest" % "2.0.M3" % "test",
	"io.netty" % "netty" % "3.5.3.Final" withSources(),
	"com.alibaba" % "fastjson" % "1.1.22" % "compile" withSources(),
	"com.notnoop.apns" % "apns" % "0.1.6" withSources(),
	"org.mockito" % "mockito-all" % "1.9.0" % "test",
	"org.mashupbots.socko" % "socko-webserver_2.9.1" % "0.2.1" withSources(),
	"com.redis" % "scala-redis" % "1.1" from "http://cloud.github.com/downloads/yubingxing/scala-redis/scala-redis_2.9.2-1.1.jar",
	"org.scalaj" % "scalaj-collection" % "2.0" from "http://cloud.github.com/downloads/yubingxing/scalaj-collection/scalaj-collection_2.9.2-2.0.jar"
)