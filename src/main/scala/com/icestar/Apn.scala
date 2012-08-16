package com.icestar
import scala.collection.mutable.HashMap

import org.slf4j.LoggerFactory

import com.icestar.utils.RedisPool
import com.notnoop.apns.ReconnectPolicy.Provided.EVERY_HALF_HOUR
import com.notnoop.apns.APNS
import com.notnoop.apns.ApnsService

/**
 * Apn utils
 * @author IceStar
 */
object Apn {
  private val APN_MAP = HashMap[String, Apn]()

  def apply(appId: String, cert: String, pass: String) = {
    var map = APN_MAP
    var apn: Apn = null
    if (map.contains(appId) && map(appId).isInstanceOf[ApnsService]) {
//      println("Get apnservice from cache.")
      apn = map(appId).asInstanceOf[Apn]
    } else {
      apn = new Apn(appId, cert, pass)
      map += (appId -> apn)
    }
    apn
  }
}
class Apn private (val appId: String, val cert: String, val pass: String) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val service = APNS.newService().withCert(cert, pass).withReconnectPolicy(EVERY_HALF_HOUR).withProductionDestination().withSandboxDestination().build()
  //  val service = APNS.newService().withCert(cert, pass).withSandboxDestination().build()

  def send(token: String, payload: String, expiry: Int = 30000) {
    if (service != null) {
      logger.info("Send message to Apple APNs", token, payload)
//      println("Send message to Apple APNs", token, payload)
      service.push(token, payload)
    }
  }

  def cleanInactiveDevies() {
    val map = service.getInactiveDevices().keySet().iterator()
    while (map.hasNext()) {
      val token = map.next
      RedisPool.hdel(Server.TOKENS + appId, token)
    }
  }
}