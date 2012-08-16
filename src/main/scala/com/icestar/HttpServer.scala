package com.icestar
import java.io.File
import java.io.FileOutputStream
import java.util.Date

import org.apache.commons.io.FileUtils
import org.jboss.netty.handler.codec.http.multipart.Attribute
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import org.jboss.netty.handler.codec.http.multipart.FileUpload
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.routes.GET
import org.mashupbots.socko.routes.HttpRequest
import org.mashupbots.socko.routes.POST
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.PathSegments
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import com.icestar.utils.CommonUtils
import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.routing.FromConfig
import utils.RedisPool

object HttpServer {
  private val actorConfig = """
    my-pinned-dispatcher {
	  type=PinnedDispatcher
	  executor=thread-pool-executor
	}
    akka {
	  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
	  loglevel=DEBUG
      actor {
	    deployment {
		  /static-file-router {
		  	router = round-robin
		  	nr-of-instances = 5
		  }
		  /file-upload-router {
		  	router = round-robin
		  	nr-of-instances = 5
		  }
		}
	  }
    }
    """
  private val actorSystem = ActorSystem("HttpServer", ConfigFactory.parseString(actorConfig))

  def apply() = {
    new HttpServer(actorSystem)
  }
}
class HttpServer private (val system: ActorSystem) extends AnyRef {
  private val staticFileHandlerRouter = system.actorOf(Props[StaticContentHandler]
    .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")

  private val fileUploadHandlerRouter = system.actorOf(Props[FileUploadHandler]
    .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "file-upload-router")
  private val conf = ConfigFactory.load

  /**
   * define routes
   */
  private val routes = Routes({
    case HttpRequest(request) => request match {
      case POST(Path("/upload")) =>
        // Save file to the upload directory so it can be downloaded
        fileUploadHandlerRouter ! FileUploadRequest(request, uploadDir)
      case GET(Path("/")) =>
        println(request.endPoint.host)
        request.response.redirect("http://" + request.endPoint.host + "/index.html")
      //      case GET(PathSegments(fileName :: Nil)) =>
      //        println("Download requested file " + fileName)
      //        // Download requested file
      //        staticFileHandlerRouter ! new StaticFileRequest(request, new File(uploadDir, fileName))
      case GET(_) =>
        // Send request to HttpHandler
        system.actorOf(Props[HttpHandler]) ! request
    }
  })

  // Create root and temp dir
  private val uploadDir: File = new File(conf.getString("HttpServer.uploadPath"))
  uploadDir mkdir
  private var tempDir: File = new File(conf.getString("HttpServer.tmpPath"))
  tempDir mkdir

  //  println("[rootFilePaths] = " + uploadDir.getAbsolutePath)
  StaticContentHandlerConfig.rootFilePaths = Seq(uploadDir.getAbsolutePath);
  StaticContentHandlerConfig.tempDir = tempDir;
  StaticContentHandlerConfig.browserCacheTimeoutSeconds = 60
  StaticContentHandlerConfig.serverCacheTimeoutSeconds = 2
  private val webServer: WebServer = new WebServer(new WebServerConfig(conf, "HttpServer"),
    routes, system)

  def start() {
    // Create content
    createContent(uploadDir)

    // Start web server
    webServer start;
    Runtime.getRuntime().addShutdownHook(new Thread {
      override def run {
        webServer stop
      }
    })
    println("WebServer starting..., http://" + webServer.config.hostname + ":" + webServer.config.port + "/")
  }

  def stop() {
    webServer.stop()
    if (staticFileHandlerRouter != null) {
      system.stop(staticFileHandlerRouter)
    }
    if (fileUploadHandlerRouter != null) {
      system.stop(fileUploadHandlerRouter)
    }
    if (tempDir != null) {
      FileUtils.deleteDirectory(tempDir)
      tempDir = null
    }
    system.shutdown()
  }

  /**
   * Creates html and css files in the specified directory
   * @param dir
   */
  private def createContent(dir: File) {
    val buf = new StringBuilder
    buf append """
    <html>
     <head>
      <title> File Upload Server </title>
      <link rel="stylesheet" type="text/css" href="mystyle.css" />
     </head>
    <body>
     <h1> File Upload Server </h1>
     <form action="upload" enctype="multipart/form-data" method="post">
      <div class="field">
       <label>1. Select a file to upload</label><br/>
       <input type="file" name="fileUpload" />
      </div>
      <div class="field">
       <label>2. Description</label><br/>
       <input type="text" name="fileDescription" size="50" />
      </div>
      <div class="field">
       <input type="submit" value="Upload" />
      </div>
     </form>
    </body>
    </html>
    """

    val indexFile = new File(dir, "index.html")
    val out = new FileOutputStream(indexFile)
    out.write(buf.toString().getBytes(CharsetUtil.UTF_8))
    out.close()

    buf.setLength(0)
    buf append "body { font-family: Arial,Helv,Courier,Serif}\n"
    buf append "div.field {margin-top: 20px;}\n"

    val cssFile = new File(dir, "mystyle.css")
    val out2 = new FileOutputStream(cssFile)
    out2.write(buf.toString().getBytes(CharsetUtil.UTF_8))
    out2.close()
  }
}

/**
 * Processes file uploads
 * @author IceStar
 */
private class FileUploadHandler extends Actor {
  private val log = Logging(context.system, this)

  def receive = {
    case msg: FileUploadRequest => {
      val ctx = msg.event
      try {
        val contentType = ctx.request.contentType
        if (contentType != "" &&
          (contentType.startsWith("multipart/form-data")) ||
          contentType.startsWith("application/x-www-form-urlencoded")) {
          val decoder = new HttpPostRequestDecoder(HttpDataFactory.value, ctx.nettyHttpRequest)

          val descriptionField = decoder.getBodyHttpData("fileDescription").asInstanceOf[Attribute]

          println("============================================")
          val uploadField = decoder.getBodyHttpData("fileUpload").asInstanceOf[FileUpload]
          println(uploadField)
          val destFile = new File(msg.saveDir, uploadField.getFilename)
          println("============================================")
          println(msg.saveDir + "//" + uploadField.getFilename())
          println("============================================")
          uploadField.renameTo(destFile)

          ctx.response.write("File upload complete!")
        } else {
          ctx.response.write(HttpResponseStatus.BAD_REQUEST)
        }
      } catch {
        case ex => {
          ctx.response.write(HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.toString())
        }
      }
    }
  }
}

private case class FileUploadRequest(event: HttpRequestEvent, saveDir: File)

/**
 * Data factory for use with 'HttpPostRequestDecoder'
 * @author IceStar
 */
private object HttpDataFactory {
  val value = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)
}
/**
 * Handle http request and writes the response then stop
 * @author IceStar
 */
private class HttpHandler extends Actor {
  def receive = {
    case event: HttpRequestEvent =>
      val response = event.response
      event match {
        case GET(PathSegments("urls" :: appId :: Nil)) =>
          response.write(CommonUtils.getOrElse(RedisPool.hget(Server.URLS, appId)))
        case _ =>
          response.write("Hello from Socko (" + new Date().toString() + ")")
      }
      context.stop(self)
  }
}