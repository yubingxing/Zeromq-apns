package com.icestar

import org.slf4j.LoggerFactory

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.JSON
import com.icestar.utils.crypto.MD5
import com.icestar.utils.CommonUtils
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
  val BACKUP = "APN_BACKUP"
  val PAYLOADS = "APN_PAYLOADS_"
  val TOKENS = "APN_TOKENS_"
  val URLS = "APN_URLS"
  private val logger = LoggerFactory.getLogger(getClass)
  var actor: ActorRef = _
  var debugMode: Boolean = _

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
    println("ApnServer starting..., " + address)
    val server = Server(system, address)
    HttpServer().start
    debugMode = conf.getString("apnserver.debugMode").toLowerCase() == "on"
    println("[debugMode] = " + debugMode)
  }
}
class Server(val address: String) extends Actor with ActorLogging {
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
  private val CMD_AUTOCLEAN_TOKENS = """autoclean_tokens (.+)""".r
  private val CMD_PUSH_MSG = """push (.+)::(.+)""".r
  private val CMD_SET_URLS = """urls (.+)::(.+)""".r
  private val CMD_GET_URLS = """urls (.+)""".r

  override def preStart() = {
    log.debug("ZMQActor Starting")
  }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }

  def println(str: Any) {
    if (Server.debugMode)
      Console println str
  }

  def receive = {
    case Connecting => println("ZMQ-APNs Server connected")
    case m: ZMQMessage =>
      val msg = m.firstFrameAsString
      println("[Receive]:: " + msg)
      var cmd: String = ""
      if (m.frames.length > 1) {
        val tmp = m.frames(1)
        cmd = tmp.payload.map(_ toChar).mkString
        println("[CMD]:: " + cmd)
      }
      //      try {
      msg match {
        case CMD_RECEIVE_TOKEN(appId, token) =>
          // receive from iphone/ipad device token
          RedisPool.hset(Server.TOKENS + appId, token, true)
        case CMD_GET_TOKENS(appId) =>
          responseOK(cmd, getTokens(appId))
        case CMD_GET_TOKENS_COUNT(appId) =>
          // get appid stored tokens count
          responseOK(cmd, RedisPool.hlen(Server.TOKENS + appId) toString)
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
            responseOK(cmd)
          case CMD_GET_APPS =>
            val data = RedisPool.hgetall(Server.APN_APPS_MAP)
            //          val content: JSONObject = new JSONObject
            //          data.foreach(e => {
            //            content.put(e._1, e._2)
            //          })
            if (data != null) {
              val content = JSON.toJSONString(data.asJava, SerializerFeature.PrettyFormat)
              responseOK(cmd, content)
            }
          case CMD_DEL_APP(appId) =>
            // backup app details
            RedisPool.hset(Server.BACKUP, appId, RedisPool.hget(Server.APN_APPS_MAP, appId))
            RedisPool.hdel(Server.APN_APPS_MAP, appId)
            // backup app payloads
            RedisPool.hset(Server.BACKUP, Server.PAYLOADS + appId, getPayloads(appId))
            RedisPool.del(Server.PAYLOADS + appId)
            // backup app tokens
            RedisPool.hset(Server.BACKUP, Server.TOKENS + appId, getTokens(appId))
            RedisPool.del(Server.TOKENS + appId)
            responseOK(cmd)
          case x => x match {
            case CMD_SET_PAYLOAD(appId, value) =>
              // set appid payload data
              val key = MD5.hash(value)
              RedisPool.hset(Server.PAYLOADS + appId, key, value)
              responseOK(cmd)
            case CMD_DEL_PAYLOAD(appId, key) =>
              RedisPool.hdel(Server.PAYLOADS + appId, key)
              responseOK(cmd)
            case CMD_GET_PAYLOADS(appId) =>
              // get all payloads
              responseOK(cmd, getPayloads(appId))
            case CMD_AUTOCLEAN_TOKENS(appId) =>
              if (RedisPool.hlen(Server.TOKENS + appId) <= 0) {
                responseOK(cmd)
              } else {
                val data = RedisPool.hget(Server.APN_APPS_MAP, appId)
                if (data != null) {
                  val content = JSON.parseObject(data)
                  val conf = ConfigFactory.load()
                  val apn = Apn(appId, conf.getString("HttpServer.uploadPath") + content.getString("cert"), content.getString("pass"))
                  if (apn != null) {
                    apn.cleanInactiveDevies()
                    responseOK(cmd)
                  } else {
                    responseFail(cmd)
                  }
                }
              }
            case x => x match {
              case CMD_SET_URLS(appId, value) =>
                RedisPool.hset(Server.URLS, appId, value)
                responseOK(cmd)
              case CMD_GET_URLS(appId) =>
                responseOK(cmd, RedisPool.hget(Server.URLS, appId))
              case x => log.warning("Received unknown message: {}", x)
            }
          }
        }
      }
    //      } catch {
    //        case e:Exception =>
    //          log.error(e getStackTraceString)
    //          e printStackTrace
    //      }
    case x => log.warning("Received unknown message: {}", x)
  }

  /**
   * get app all payloads of the app
   * @param appId
   */
  private[this] def getPayloads(appId: String): String = {
    val data = RedisPool.hgetall(Server.PAYLOADS + appId)
    if (data != null && data != "") {
      return JSON.toJSONString(data asJava, SerializerFeature.PrettyFormat)
    }
    return null
  }

  /**
   * get all tokens of the app
   * @param appId
   * @return
   */
  private[this] def getTokens(appId: String): String = {
    val data = RedisPool.hkeys(Server.TOKENS + appId)
    if (data != null && data != "") {
      return JSON.toJSONString(data asJava, SerializerFeature.PrettyFormat)
    }
    return null
  }

  private[this] def responseOK(cmd: String, data: String = "") = {
    repSocket ! ZMQMessage(Seq(Frame("OK"), Frame(CommonUtils.getOrElse(cmd)), Frame(CommonUtils.getOrElse(data))))
  }

  private[this] def responseFail(cmd: String, data: String = "") = {
    repSocket ! ZMQMessage(Seq(Frame("Fail"), Frame(CommonUtils.getOrElse(cmd)), Frame(CommonUtils.getOrElse(data))))
  }
}
