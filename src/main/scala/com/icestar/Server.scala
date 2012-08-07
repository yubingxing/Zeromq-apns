package com.icestar

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.zeromq.zeromqSystem
import akka.zeromq.Bind
import akka.zeromq.Connecting
import akka.zeromq.Frame
import akka.zeromq.Listener
import akka.zeromq.SocketType
import akka.zeromq.ZMQMessage

/**
 * Server boot class
 * @author IceStar
 */
object Server {
  private val logger = LoggerFactory.getLogger(getClass)

  def apply(system: ActorSystem, address: String) = {
    logger.info("Creating Sockets...")
    val server = system.actorOf(Props(new Server(address)), "Server")
    logger.info("Socket created success.")
    server
  }

  def main(args: Array[String]) = {
    logger.info("Starting ZMQ-APNs server...")
    val system = ActorSystem("apnserver")
    //    Conf read args.head
    logger.info("Reading configure...")
    val conf = ConfigFactory.load();
    RedisPool.init(conf.getString("redis.host"), conf.getInt("redis.port"))
    val address = conf.getString("apnserver.address")
    println("Connecting to " + address)
    val server = Server(system, address)
    val contentServer = StaticContentServer()
    contentServer start;
  }
}
private class Server(address: String) extends Actor with ActorLogging {
  private val APN_GAMES_MAP = "APN_GAMES_MAP"
  private val repSocket = context.system.newSocket(SocketType.Rep, Bind(address), Listener(self))
  private val SetCommand = """set (.+)::(.+)""".r
  private val APNCommand = """apnset (.+)""".r
  private val GetCommand = """get (.+)""".r
  private val GetAllGameIds = "getAllGameIds"
  private val GetAllGameContents = "getAllGameContents"

  override def preStart() = {
    log.debug("ZMQActor Starting")
  }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }

  def receive = {
    case Connecting => println("ZMQ-APNs Server connected")
    case m: ZMQMessage =>
      val msg = m.firstFrameAsString
      println("[Receive]:: " + msg)
      msg match {
        case SetCommand(gameId, data) =>
          RedisPool.hset("APN_GAMES_MAP", gameId, data)
          repSocket ! ZMQMessage(Seq(Frame("ok")))
        case APNCommand(key, value) =>
          RedisPool.set(key, value)
          repSocket ! ZMQMessage(Seq(Frame("ok")))
        case GetCommand(key) =>
          repSocket ! ZMQMessage(Seq(Frame(RedisPool.hget(APN_GAMES_MAP, key))))
        case GetAllGameIds =>
          val data = RedisPool.hkeys(APN_GAMES_MAP)
          var seq: Seq[Frame] = Seq()
          data.foreach(s => {
            seq ++= Seq(Frame(s))
          })
          repSocket ! ZMQMessage(seq)
        case GetAllGameContents =>
          val data = RedisPool.hvals(APN_GAMES_MAP)
          var seq: Seq[Frame] = Seq()
          data.foreach(s => {
            seq ++= Seq(Frame(s))
          })
          repSocket ! ZMQMessage(seq)
        case _ => sender ! ZMQMessage(Seq(Frame("error")))
      }
    case x => log.warning("Received unknown message: {}", x)
  }
}