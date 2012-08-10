package com.icestar

import org.slf4j.LoggerFactory

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.JSON
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
import scalaj.collection.Imports.RichSMap
import scalaj.collection.Imports.RichSSeq

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
class Server(address: String) extends Actor with ActorLogging {
  private val repSocket = context.system.newSocket(SocketType.Rep, Bind(address), Listener(self))
  //***************************CONSTANTS****************************//
  private val APN_APPS_MAP = "APN_APPS_MAP"
  private val APN_APPS_PAYLOADS = "APN_APPS_PAYLOADS"
  private val TOKENS = "TOKENS_"
  //**************************MSG COMMANDS*****************************// 
  private val CMD_SET_APP = """app (.+)::(.+)""".r
  private val CMD_GET_ALL_APPS = "getapps"
  private val CMD_SET_PAYLOAD = """payload (.+)""".r
  private val CMD_RECEIVE_TOKEN = """tOKen (.+)::(.+)""".r
  private val CMD_GET_TOKENS = """tOKens (.+)""".r
  private val CMD_GET_TOKENS_COUNT = """tOKens_count (.+)""".r
  private val CMD_SEND_MSG = """send (.+)::(.+)""".r

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
        case CMD_RECEIVE_TOKEN(appId, tOKen) =>
          // receive from iphone/ipad device tOKen
          RedisPool.hset(TOKENS + appId, tOKen, true)
          repSocket ! ZMQMessage(Seq(Frame("OK")))
        case CMD_GET_TOKENS(appId) =>
          val data = RedisPool.hkeys(TOKENS + appId)
          if (data != null) {
            val list = data asJava
            val content: String = JSON.toJSONString(list, SerializerFeature.PrettyFormat)
            // val content: String = JSON.toJSONString(list, {SerializerFeature.QuoteFieldNames;SerializerFeature.PrettyFormat})
            println(content)
            repSocket ! ZMQMessage(Seq(Frame(content)))
          }
        case CMD_GET_TOKENS_COUNT(appId) =>
          // get appid stored tOKens count
          repSocket ! ZMQMessage(Seq(Frame(RedisPool.hlen(TOKENS + appId) toString)))
        case CMD_SEND_MSG(appId, key) =>
        // send msg to all the stored device tOKens of the appId

        case CMD_SET_APP(appId, data) =>
          // set appid base data
          RedisPool.hset(APN_APPS_MAP, appId, data)
          repSocket ! ZMQMessage(Seq(Frame("OK")))
        case CMD_GET_ALL_APPS =>
          val data = RedisPool.hgetall(APN_APPS_MAP)
          //          val content: JSONObject = new JSONObject
          //          data.foreach(e => {
          //            content.put(e._1, e._2)
          //          })
          if (data != null) {
            val content = JSON.toJSONString(data.asJava, SerializerFeature.PrettyFormat)
            println(content)
            repSocket ! ZMQMessage(Seq(Frame(content)))
          }
        case CMD_SET_PAYLOAD(key, value) =>
          // set appid payload data
          RedisPool.set(key, value)
          repSocket ! ZMQMessage(Seq(Frame("OK")))
        //        case CMD_GET_ALL_PAYLOADS =>
        //          val data = RedisPool.hvals(APN_APPS_MAP)
        //          var seq: Seq[Frame] = Seq()
        //          data.foreach(s => {
        //            seq ++= Seq(Frame(s))
        //          })
        //          repSocket ! ZMQMessage(seq)
        case _ => sender ! ZMQMessage(Seq(Frame("error")))
      }
    case x => log.warning("Received unknown message: {}", x)
  }
}