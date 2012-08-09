package com.icestar

import org.slf4j.LoggerFactory

import com.icestar.utils.RedisPool
import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
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
  var actor: ActorRef = _

  def apply(system: ActorSystem, address: String) = {
    logger.info("Creating Sockets...")
    actor = system.actorOf(Props(new Server(address)), "Server")
    logger.info("Socket created success.")
    actor
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
  //*******************************************************//
  private val APN_APPS_MAP = "APN_APPS_MAP"
  private val APN_APPS_PAYLOADS = "APN_APPS_PAYLOADS"
  private val TOKENS = "TOKENS_"
  //*******************************************************// 
  private val repSocket = context.system.newSocket(SocketType.Rep, Bind(address), Listener(self))
  private val CMD_SET_APP = """set (.+)::(.+)""".r
  private val CMD_SET_PAYLOAD = """apnset (.+)""".r
  private val CMD_RECEIVE_TOKEN = """token (.+)::(.+)""".r
  private val CMD_GET_TOKENS = """tokens (.+)""".r
  private val CMD_GET_TOKENS_COUNT = """tokens_count (.+)""".r
  private val CMD_SEND_MSG = """send (.+)::(.+)""".r
  private val CMD_GET_ALL_APPS = "getAllGameIds"
  private val CMD_GET_ALL_PAYLOADS = "getAllGameContents"

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
        case CMD_RECEIVE_TOKEN(appId, token) =>
          // receive from iphone/ipad device token
          RedisPool.hset(TOKENS + appId, token, true)
          repSocket ! ZMQMessage(Seq(Frame("ok")))
        case CMD_GET_TOKENS(appId) =>

        case CMD_GET_TOKENS_COUNT(appId) =>
          // get appid stored tokens count
          repSocket ! ZMQMessage(Seq(Frame(RedisPool.hlen(TOKENS + appId) toString)))
        case CMD_SEND_MSG(appId, key) =>

        case CMD_SET_APP(appId, data) =>
          // set appid base data
          RedisPool.hset(APN_APPS_MAP, appId, data)
          repSocket ! ZMQMessage(Seq(Frame("ok")))
        case CMD_SET_PAYLOAD(key, value) =>
          // set appid payload data
          RedisPool.set(key, value)
          repSocket ! ZMQMessage(Seq(Frame("ok")))
        case CMD_GET_ALL_APPS =>
          val data = RedisPool.hkeys(APN_APPS_MAP)
          var seq: Seq[Frame] = Seq()
          data.foreach(s => {
            seq ++= Seq(Frame(s))
          })
          repSocket ! ZMQMessage(seq)
        case CMD_GET_ALL_PAYLOADS =>
          val data = RedisPool.hvals(APN_APPS_MAP)
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