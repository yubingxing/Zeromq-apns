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
  private var map = HashMap[String, Apn]()

  def apply(gameId: String, cert: String, pass: String) = {
    var apn: Apn = null
    if (map.contains(gameId) && map(gameId).isInstanceOf[ApnsService]) {
      println("Get apnservice from cache.")
      apn = map(gameId).asInstanceOf[Apn]
    } else {
      apn = new Apn(gameId, cert, pass)
      map += (gameId -> apn)
    }
    apn
  }

}
class Apn private (val gameId: String, val cert: String, val pass: String) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val service = APNS.newService().withCert(cert, pass).withReconnectPolicy(EVERY_HALF_HOUR).withProductionDestination().withSandboxDestination().build()
  //  val service = APNS.newService().withCert(cert, pass).withSandboxDestination().build()

  def send(token: String, payload: String, expiry: Int = 30000) {
    if (service != null) {
      logger.info("Send message to Apple APNs", token, payload)
      println("Send message to Apple APNs", token, payload)
      service.push(token, payload)
    }
  }

  def cleanInactiveDevies() {
    val map = service.getInactiveDevices().keySet().iterator()
    while (map.hasNext()) {
      val token = map.next
      RedisPool.hdel(gameId, token)
    }
  }
}