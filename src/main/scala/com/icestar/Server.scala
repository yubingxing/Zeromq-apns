package com.icestar

import org.slf4j.LoggerFactory

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.JSON
import com.icestar.utils.crypto.MD5
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
  //***************************CONSTANTS****************************//
  val APN_APPS_MAP = "APN_APPS_MAP"
  val PAYLOADS = "PAYLOADS_"
  val TOKENS = "TOKENS_"
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
  //**************************MSG COMMANDS*****************************// 
  private val CMD_SET_APP = """app (.+)::(.+)""".r
  private val CMD_DEL_APP = """delapp (.+)""".r
  private val CMD_GET_APPS = "apps"
  private val CMD_SET_PAYLOAD = """payload (.+)::(.+)""".r
  private val CMD_DEL_PAYLOAD = """delpayload (.+)::(.+)""".r
  private val CMD_GET_PAYLOADS = """payloads (.+)""".r
  private val CMD_RECEIVE_TOKEN = """token (.+)::(.+)""".r
  private val CMD_GET_TOKENS = """tokens (.+)""".r
  private val CMD_GET_TOKENS_COUNT = """tokens_count (.+)""".r
  private val CMD_AUTOCLEAN_TOKENS = """autoclean_tokens (.+)::(.+)::(.+)""".r
  private val CMD_PUSH_MSG = """push (.+)::(.+)""".r

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
          RedisPool.hset(Server.TOKENS + appId, token, true)
        case CMD_GET_TOKENS(appId) =>
          val data = RedisPool.hkeys(Server.TOKENS + appId)
          if (data != null) {
            val list = data asJava
            val content: String = JSON.toJSONString(list, SerializerFeature.PrettyFormat)
            // val content: String = JSON.toJSONString(list, {SerializerFeature.QuoteFieldNames;SerializerFeature.PrettyFormat})
            println(content)
            repSocket ! ZMQMessage(Seq(Frame(content)))
          }
        case CMD_GET_TOKENS_COUNT(appId) =>
          // get appid stored tokens count
          repSocket ! ZMQMessage(Seq(Frame(RedisPool.hlen(Server.TOKENS + appId) toString)))
        case CMD_PUSH_MSG(appId, key) =>
          // send msg to all the stored device tokens of the appId
          val content = RedisPool.hget(Server.PAYLOADS + appId, key)
          if (content != null) {
            val payload = JSON.parseObject(content) getString "payload"
            val tokens = RedisPool.hkeys(Server.TOKENS + appId)
            if (payload != null && tokens != null) {
              val data = JSON.parseObject(content)
              val apn = Apn(appId, data.getString("cert"), data.getString("passwd"))
              if (apn != null)
                tokens map (apn send (_, payload))
            }
          }
        case x => x match {
          case CMD_SET_APP(appId, data) =>
            // set appid base data
            RedisPool.hset(Server.APN_APPS_MAP, appId, data)
            repSocket ! ZMQMessage(Seq(Frame("OK")))
          case CMD_DEL_APP(appId) =>
            RedisPool.hdel(Server.APN_APPS_MAP, appId)
            repSocket ! ZMQMessage(Seq(Frame("OK")))
          case CMD_GET_APPS =>
            val data = RedisPool.hgetall(Server.APN_APPS_MAP)
            //          val content: JSONObject = new JSONObject
            //          data.foreach(e => {
            //            content.put(e._1, e._2)
            //          })
            if (data != null) {
              val content = JSON.toJSONString(data.asJava, SerializerFeature.PrettyFormat)
              repSocket ! ZMQMessage(Seq(Frame(content)))
            }
          case CMD_SET_PAYLOAD(appId, value) =>
            // set appid payload data
            val key = MD5.hash(value)
            RedisPool.hset(Server.PAYLOADS + appId, key, value)
            repSocket ! ZMQMessage(Seq(Frame("OK")))
          //        case CMD_GET_ALL_PAYLOADS =>
          //          val data = RedisPool.hvals(APN_APPS_MAP)
          //          var seq: Seq[Frame] = Seq()
          //          data.foreach(s => {
          //            seq ++= Seq(Frame(s))
          //          })
          //          repSocket ! ZMQMessage(seq)
          case CMD_DEL_PAYLOAD(appId, key) =>
            RedisPool.hdel(Server.PAYLOADS + appId, key)
            repSocket ! ZMQMessage(Seq(Frame("OK")))
          case CMD_GET_PAYLOADS(appId) =>
            // get all payloads
            val data = RedisPool.hgetall(Server.PAYLOADS + appId)
            if (data != null) {
              val content = JSON.toJSONString(data asJava, SerializerFeature.PrettyFormat)
              repSocket ! ZMQMessage(Seq(Frame(content)))
            }
          case CMD_AUTOCLEAN_TOKENS(appId, cert, pass) =>
            val apn = Apn(appId, cert, pass)
            if (apn != null) {
              apn.cleanInactiveDevies()
              repSocket ! ZMQMessage(Seq(Frame("OK")))
            } else {
              repSocket ! ZMQMessage(Seq(Frame("Fail")))
            }
          case x => log.warning("Received unknown message: {}", x)
        }
      }
    case x => log.warning("Received unknown message: {}", x)
  }
}
