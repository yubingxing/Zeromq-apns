import sbt._
import sbt.Keys._

object ZeromqapnsBuild extends Build {

  lazy val zeromqapns = Project(
    id = "zeromq-apns",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Zeromq-apns",
      organization := "com.icestar",
      version := "1.0",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.2"
    )
  )
}
