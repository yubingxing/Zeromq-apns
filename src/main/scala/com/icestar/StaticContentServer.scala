package com.icestar
import java.io.File

import org.apache.commons.io.FileUtils
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

import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig

object StaticContentServer {
  private val actorSystem = ActorSystem("staticContentServer")

  def apply() = {
    new StaticContentServer(actorSystem)
  }
}
class StaticContentServer private (system: ActorSystem) extends AnyRef {
  private val routes = Routes({
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
  private var rootDir: File = null
  private var tempDir: File = null
  private var router: ActorRef = null
  private var webServer: WebServer = null
  private val conf = ConfigFactory.load
  var path: String = null

  def start() {
    // Create root and temp dir
    rootDir = new File(conf.getString("staticContentServer.rootPath"))
    rootDir.mkdir()
    tempDir = new File(conf.getString("staticContentServer.tmpPath"))
    tempDir.mkdir()

    StaticContentHandlerConfig.rootFilePaths = Seq(rootDir.getAbsolutePath);
    StaticContentHandlerConfig.tempDir = tempDir;
    StaticContentHandlerConfig.browserCacheTimeoutSeconds = 60
    StaticContentHandlerConfig.serverCacheTimeoutSeconds = 2

    // Start routers 
    router = system.actorOf(Props[StaticContentHandler].
      withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "my-router")

    // Start web server
    webServer = new WebServer(new WebServerConfig(conf, "staticContentServer"),
      routes, system)
    path = "http://" + webServer.config.hostname + ":" + webServer.config.port + "/"
    webServer.start()
  }

  def stop() {
    webServer.stop()
    if (router != null) {
      system.stop(router)
      router = null
    }
    if (tempDir != null) {
      FileUtils.deleteDirectory(tempDir)
      tempDir = null
    }
    system.shutdown()
  }
}