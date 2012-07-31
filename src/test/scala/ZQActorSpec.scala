import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec

import com.icestar.Conf
import com.icestar.RedisPool
import com.icestar.Server

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.zeromq.zeromqSystem
import akka.zeromq.Connect
import akka.zeromq.Connecting
import akka.zeromq.Frame
import akka.zeromq.Listener
import akka.zeromq.SocketType
import akka.zeromq.ZMQMessage

class ZMQActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSpec with BeforeAndAfter {
  def this() = this(ActorSystem("ZQActorSpec"))

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

  it("should respond with ok to valid message") {
    println("-----------------------------------")
//    val actor = TestActorRef(new ZMQActor(address))
    Server(_system, address)

    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))

    expectMsg(Connecting)

    socket ! ZMQMessage(Seq(Frame("""set fishgame::{"name":"fishgame", "id":"fishgame", "passwd":"fishgame"}""")))

    expectMsg(ZMQMessage(Seq(Frame("ok"))))
    
  }

  it("should respond with error to unrecognized message") {
    println("-----------------------------------")
//    val actor = TestActorRef(new ZMQActor(address))
    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))

    expectMsg(Connecting)

    socket ! ZMQMessage(Seq(Frame("This's a error message.")))

    expectMsg(ZMQMessage(Seq(Frame("error"))))
  }

  it("should save value to appropriate key") {
    println("-----------------------------------")
//    val actor = TestActorRef(new ZMQActor(address))
    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
    
    expectMsg(Connecting)

    socket ! ZMQMessage(Seq(Frame("set name::IceStar")))
    socket ! ZMQMessage(Seq(Frame("set email::yubingxing123@gmail.com")))

    assert(RedisPool.hget("APN_GAMES_MAP", "name") === "IceStar")
    assert(RedisPool.hget("APN_GAMES_MAP", "email") === "yubingxing123@gmail.com")
  }

  it("should get value from appropriate key") {
    println("-----------------------------------")
//    val actor = TestActorRef(new ZMQActor(address))
    val socket = system.newSocket(SocketType.Req, Connect(client_address), Listener(self))
    
    expectMsg(Connecting)

    socket ! ZMQMessage(Seq(Frame("get name")))
    socket ! ZMQMessage(Seq(Frame("get email")))
    
//    expectMsg(actor ! ZMQMessage(Seq(Frame("get name"))))
//    expectMsg(actor ! ZMQMessage(Seq(Frame("get email"))))
  }
}