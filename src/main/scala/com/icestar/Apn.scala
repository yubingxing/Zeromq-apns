package com.icestar
import org.slf4j.LoggerFactory

import com.notnoop.apns.ReconnectPolicy.Provided.EVERY_HALF_HOUR
import com.notnoop.apns.APNS
import com.notnoop.apns.ApnsService

/**
 * Apn utils
 * @author IceStar
 */
object Apn {
  val logger = LoggerFactory.getLogger(getClass)
  var map = Map[String, ApnsService]()
  var service: ApnsService = _

  def apply(gameId: String, cert: String, pass: String, devMode: Boolean) = {
    if (map(gameId).isInstanceOf[ApnsService]) {
      map(gameId).asInstanceOf[ApnsService]
    } else {
      map += (gameId -> APNS.newService().withCert(cert, pass).withReconnectPolicy(EVERY_HALF_HOUR).withProductionDestination().build())
    }
  }

  def send(token: String, payload: String, expiry: Int) {
    if (service != null) {
      logger.info("Send message to Apple APNs", token, payload)
      service.push(token, payload)
    }
  }
}