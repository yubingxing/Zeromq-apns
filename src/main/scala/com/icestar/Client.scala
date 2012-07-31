package com.icestar
import org.slf4j.LoggerFactory

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.zeromq.zeromqSystem
import akka.zeromq.Connect
import akka.zeromq.Frame
import akka.zeromq.Listener
import akka.zeromq.SocketType
import akka.zeromq.ZMQMessage

object Client {
  val logger = LoggerFactory.getLogger(getClass)
  def apply(system: ActorSystem, address: String) = {
    logger.info("Creating Sockets...")
    val client = system.actorOf(Props(new Client(address)), "Client")
    logger.info("Socket created success.")
    client
  }

  def test() = {
    val system = ActorSystem("ZMQ-CLIENT")
    val client = Client(system, "tcp://127.0.0.1:5566")
    client ! """set hahahaha::{"test":"testesttest"}"""
    system.shutdown()
  }
}

class Client(address: String) extends Actor {
  val request = context.system.newSocket(SocketType.Req, Connect(address), Listener(self))
  def receive = {
    case m: String =>
      println("[Send]:: " + m)
      request ! ZMQMessage(Seq(Frame(m)))
  }
}