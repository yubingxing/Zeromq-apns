package com.icestar

import scala.util.matching.Regex
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.zeromq.zeromqSystem
import akka.zeromq.Bind
import akka.zeromq.Connecting
import akka.zeromq.Frame
import akka.zeromq.Listener
import akka.zeromq.SocketType
import akka.zeromq.ZMQMessage
import akka.event.Logging

case class WatchKey(key: String, watcher: ActorRef)

class ZMQActor(address: String) extends Actor with ActorLogging {
  val socket = context.system.newSocket(SocketType.Rep, Bind(address), Listener(self))
  val setCommand = new Regex("set (\\w+) (\\w+)")
  val getCommand = new Regex("get (\\w+)")

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
      println("============== Received MSG from Client ===============")
      println(m.firstFrameAsString)
      println("========================================================")
      m.firstFrameAsString match {
        case setCommand(key, value) =>
          println(key, value)
          RedisPool.set(key, value)
          sender ! ZMQMessage(Seq(Frame("ok")))
        case getCommand(key) =>
          println(RedisPool.get(key))
        case _ => sender ! ZMQMessage(Seq(Frame("error")))
      }
    case x => log.warning("Received unknown message: {}", x)
  }
}