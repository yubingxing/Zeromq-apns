name := "Zeromq-apns"

version := "1.0"

scalaVersion := "2.9.2"

resolvers ++= Seq(
	Classpaths.typesafeResolver,
	"Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
	"Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

libraryDependencies ++= Seq(
	"redis.clients" % "jedis" % "2.0.0",
	"ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime",
	"org.slf4j" % "slf4j-api" % "1.6.4",
	"com.typesafe.akka" % "akka-actor" % "2.0.2",
	"com.typesafe.akka" % "akka-zeromq" % "2.0.2",
	"com.typesafe.akka" % "akka-remote" % "2.0.2",
	"com.alibaba" % "fastjson" % "1.1.22" % "compile",
	"org.functionaljava" % "functionaljava" % "3.1",
	"com.notnoop.apns" % "apns" % "0.1.6",
	"junit" % "junit" % "4.7" % "test",
	"org.scalatest" % "scalatest_2.9.0" % "1.8" % "test",
	"com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
	"org.mockito" % "mockito-all" % "1.9.0" % "test"
)