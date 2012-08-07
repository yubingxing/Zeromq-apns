package com.icestar
import java.io.File
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.handlers.StaticFileRequest
import org.mashupbots.socko.handlers.StaticResourceRequest
import org.mashupbots.socko.routes.GET
import org.mashupbots.socko.routes.PathSegments
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

object StaticContentServer {
  val actorSystem = ActorSystem("staticContentServer")
  val routes = Routes({
    case event @ GET(PathSegments("files" :: relativePath)) => {
      val request = new StaticFileRequest(
        event.asInstanceOf[HttpRequestEvent],
        new File(rootDir, relativePath.mkString("/", "/", "")))
      router ! request
    }
    case event @ GET(PathSegments("resource" :: relativePath)) => {
      val request = new StaticResourceRequest(
        event.asInstanceOf[HttpRequestEvent],
        relativePath.mkString("", "/", ""))
      router ! request
    }
  })
  var rootDir: File = null
  var tempDir: File = null
  var router: ActorRef = null
  var webServer: WebServer = null

  def apply() = {
    val conf = ConfigFactory.load
    // Create root and temp dir
    println(conf.getString("staticContentServer.rootPath"))
    rootDir = File.createTempFile(conf.getString("staticContentServer.rootPath"), "")
    if (!rootDir.exists())
      rootDir.mkdir()
    tempDir = File.createTempFile(conf.getString("staticContentServer.tmpPath"), "")
    tempDir.delete()
    tempDir.mkdir()

    StaticContentHandlerConfig.rootFilePaths = Seq(rootDir.getAbsolutePath);
    StaticContentHandlerConfig.tempDir = tempDir;
    StaticContentHandlerConfig.browserCacheTimeoutSeconds = 60
    StaticContentHandlerConfig.serverCacheTimeoutSeconds = 2

    // Start routers 
    router = actorSystem.actorOf(Props[StaticContentHandler].
      withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"))

    // Start web server
    webServer = new WebServer(new WebServerConfig(conf, "staticContentServer"),
      routes, actorSystem)
  }
}