name := "Zeromq-apns"

version := "1.0"

scalaVersion := "2.9.2"

resolvers ++= Seq(
	Classpaths.typesafeResolver,
	"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime" withSources(),
	"commons-pool" % "commons-pool" % "1.6",
	"org.slf4j" % "slf4j-api" % "1.6.4" withSources(),
	"com.typesafe.akka" % "akka-kernel" % "2.0.2" withSources(),
	"com.typesafe.akka" % "akka-actor" % "2.0.2" withSources(),
	"com.typesafe.akka" % "akka-zeromq" % "2.0.2" withSources(),
	"com.typesafe.akka" % "akka-remote" % "2.0.2" withSources(),
	"com.alibaba" % "fastjson" % "1.1.22" % "compile" withSources(),
	"com.notnoop.apns" % "apns" % "0.1.6" withSources(),
	"org.scalatest" % "scalatest_2.9.0" % "1.8" % "test",
	"com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
	"org.mockito" % "mockito-all" % "1.9.0" % "test",
	"com.redis" % "scala-redis" % "1.0" from "http://cloud.github.com/downloads/yubingxing/scala-redis/scala-redis_2.9.2-1.0.jar"
)