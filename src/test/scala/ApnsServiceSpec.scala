import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec

import com.icestar.Apn
import com.icestar.Conf
import com.icestar.FileUtils
import com.icestar.RedisPool
import com.icestar.StaticContentServer
import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

class ApnsServiceSpec(_system: ActorSystem) extends TestKit(_system) with ShouldMatchers with ImplicitSender with FunSpec with BeforeAndAfter with TestHttpClient with Logger {
  def this() = this(ActorSystem("ApnsServiceSpec"))

  val address = "tcp://0.0.0.0:5566"
  val client_address = "tcp://127.0.0.1:5566"

  after {
    //    RedisPool.flushdb()
  }

  before {
    RedisPool.init("127.0.0.1", 6379)
  }

  it("should save value by RedisPool") {
    println("-----------------------------------")
    println(RedisPool.host, RedisPool.port)
    RedisPool.set("test", "testing")
    println(RedisPool.get("test"))
    assert(RedisPool.get("test") === "testing")
  }

  it("Should reading conf data") {
    println("-----------------------------------")
    Conf.read("src/main/resources/conf")
    assert(Conf.get("address") === "tcp://0.0.0.0:5566")
    assert(Conf.get("redis", "host") === "127.0.0.1")
    assert(Conf.get("redis", "port") === 6379)
  }

  it("should send push notifications success") {
    val apn = Apn("com.huale.PushNotificatinsDemo", "cert/pushdemo_aps.p12", "huale@hefei.")
    var token = "44c551bb46d9e0e6de47feb1c2e3b0acaf1ac797ba6d39bc2d28f228f691042b"
    val payload = """{
	    "aps" : {
	        "alert" : "This is a test notifications.",
	        "badge" : 9,
	        "sound" : "bingbong.aiff"
		    },
		    "acme1" : "bar",
		    "acme2" : 42
		}"""
    //    val payload = APNS.newPayload().alertBody("This is a test notifications.").build();
    apn.send(token, payload);

//    token = "7f3addce7e9d7780eae3bc099d08c68144f93f11b4f6a645fdf5eaa65ab28617"
//    apn.send(token, payload);
  }

  it("should correctly HTTP GET a small file") {
    val contentServer = StaticContentServer()
    contentServer start
    val rootDir = new File(ConfigFactory.load().getString("staticContentServer.rootPath"));
    val content = "test data test data test data"
    val file = new File(rootDir, "gettext1.txt")
    FileUtils.writeTextFile(file, content)
    println(contentServer.path)
    val url = new URL(contentServer.path + "files/gettext1.txt")
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    val resp = getResponseContent(conn)
    log.debug(resp.toString)

    resp.status should equal("200")
    resp.content should equal(content)
    resp.headers("Date").length should be > 0
    resp.headers("Content-type") should equal("text/plain")
    resp.headers("Cache-Control") should equal("private, max-age=60")

    val fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    fmt setTimeZone TimeZone.getTimeZone("GMT")
    resp.headers("Last-Modified") should equal(fmt.format(new Date(file.lastModified())))

    val x = resp.headers("Date")
    val date = fmt.parse(resp.headers("Date"))
    val expires = fmt.parse(resp.headers("Expirres"))
    (expires.getTime - date.getTime) should equal(StaticContentHandlerConfig.browserCacheTimeoutSeconds * 1000)
  }
  //  it("should respond with ok to valid message") {
  //    println("-----------------------------------")
  ////    val actor = TestActorRef(new ZMQActor(address))
  //    Server(_system, address)
  //
  //    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
  //
  //    expectMsg(Connecting)
  //
  //    socket ! ZMQMessage(Seq(Frame("""set fishgame::{"name":"fishgame", "id":"fishgame", "passwd":"fishgame"}""")))
  //
  //    expectMsg(ZMQMessage(Seq(Frame("ok"))))
  //    
  //  }
  //
  //  it("should respond with error to unrecognized message") {
  //    println("-----------------------------------")
  ////    val actor = TestActorRef(new ZMQActor(address))
  //    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
  //
  //    expectMsg(Connecting)
  //
  //    socket ! ZMQMessage(Seq(Frame("This's a error message.")))
  //
  //    expectMsg(ZMQMessage(Seq(Frame("error"))))
  //  }
  //
  //  it("should save value to appropriate key") {
  //    println("-----------------------------------")
  ////    val actor = TestActorRef(new ZMQActor(address))
  //    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
  //    
  //    expectMsg(Connecting)
  //
  //    socket ! ZMQMessage(Seq(Frame("set name::IceStar")))
  //    socket ! ZMQMessage(Seq(Frame("set email::yubingxing123@gmail.com")))
  //
  //    assert(RedisPool.hget("APN_GAMES_MAP", "name") === "IceStar")
  //    assert(RedisPool.hget("APN_GAMES_MAP", "email") === "yubingxing123@gmail.com")
  //  }
  //
  //  it("should get value from appropriate key") {
  //    println("-----------------------------------")
  ////    val actor = TestActorRef(new ZMQActor(address))
  //    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
  //    
  //    expectMsg(Connecting)
  //
  //    socket ! ZMQMessage(Seq(Frame("get name")))
  //    socket ! ZMQMessage(Seq(Frame("get email")))
  //    
  ////    expectMsg(actor ! ZMQMessage(Seq(Frame("get name"))))
  ////    expectMsg(actor ! ZMQMessage(Seq(Frame("get email"))))
  //  }
}