package com.icestar

import akka.actor.ActorSystem
import akka.actor.Props
import org.slf4j.LoggerFactory

/**
 * Server boot class
 * @author IceStar
 */
object Server {
  val logger = LoggerFactory.getLogger(getClass)
  
  def apply(system: ActorSystem, address: String) = {
    logger.info("Creating Sockets...")
    val server = system.actorOf(Props(new ZMQActor(address)), "Server")
    logger.info("Socket created success.")
    server
  }

  def main(args: Array[String]) = {
    logger.info("Starting ZMQ-APNs server...")
    val system = ActorSystem("ZMQ-APNs")
    //    Conf read args.head
    logger.info("Reading configure...")
    Conf.read("src/main/resources/conf")
    RedisPool.init(Conf.get("redis", "host").asInstanceOf[String], Conf.get("redis", "port").asInstanceOf[Int])
    val server = Server(system, Conf.get("address").asInstanceOf[String])
  }
}