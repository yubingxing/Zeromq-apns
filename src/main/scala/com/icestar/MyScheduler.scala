package com.icestar

import java.util.Date

import scala.collection.mutable.HashMap

import org.slf4j.LoggerFactory

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.icestar.utils.RedisPool

import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.util.duration.intToDurationInt
import akka.util.duration.longToDurationLong
import akka.zeromq.Frame
import akka.zeromq.ZMQMessage

private object ScheduleMap extends HashMap[String, Cancellable]

object MyScheduler {
  private val logger = LoggerFactory.getLogger(getClass)
  private val actorSystem = ActorSystem("MyScheduler")

  /**
   * content is json string
   * {"ct":1344420078855, "et":1344450078855, "ctTime":1344420078855, "intval":600000, "alert":"xxxxxx", "badge":3, "sound":""}
   * @param content
   */
  def apply(appId: String, key: String) = {
    new MyScheduler(actorSystem, appId, key)
  }
}

class MyScheduler private (system: ActorSystem, appId: String, key: String) extends AnyRef {
  private val _key = appId + key
  val json: String = RedisPool.hget(Server.PAYLOADS + appId, key).asInstanceOf[String]
  var sdl: Cancellable = ScheduleMap.get(_key) get;
  var content: JSONObject = _

  if (json != "" || json != null) {
    content = JSON.parseObject(json)
  }

  def start() = {
    if (content != null) {
      stop()
      val now = new Date().getTime
      val ct = content.getLongValue("ct")
      val intval = content.getIntValue("intval").seconds
      sdl = system.scheduler.schedule((if (now > ct) 0 else ct - now) milliseconds, intval, Server.actor, ZMQMessage(Seq(Frame("push " + appId + "::" + key))))
      
      val et = (now - content.getLongValue("et")).milliseconds
      sdl = system.scheduler.scheduleOnce(et, Server.actor, ZMQMessage(Seq(Frame("stop " + appId + "::" + key))))
      ScheduleMap += (_key -> sdl)

      content.put("active", true)
      RedisPool.hset(Server.PAYLOADS + appId, key, content toJSONString)
    }
  }

  def stop() = {
    if (sdl != null) {
      sdl cancel;
      content.put("active", false)
      RedisPool.hset(Server.PAYLOADS + appId, key, content toJSONString)
    }
  }

  def delete() = {
    stop()
    RedisPool.hdel(Server.PAYLOADS + appId, key)
  }
}