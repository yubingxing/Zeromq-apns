import sbt._
import Keys._

object ZeromqapnsBuild extends Build {
  lazy val copyDependencies = TaskKey[Unit]("pack")

  def copyDepTask = copyDependencies <<= (update, crossTarget, scalaVersion) map {
    (updateReport, out, scalaVer) =>
      updateReport.allFiles foreach {
        srcPath =>
          val destPath = out / "lib" / srcPath.getName
          IO.copyFile(srcPath, destPath, preserveLastModified = true)
      }
  }

  lazy val zeromqapns = Project(
    id = "Zeromq-apns",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      copyDepTask,
      name := "Zeromq-apns",
      organization := "com.icestar",
      version := "1.0",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"))
}
