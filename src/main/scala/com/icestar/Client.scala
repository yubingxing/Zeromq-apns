package com.icestar

import org.slf4j.LoggerFactory

import akka.actor.ActorSystem
import akka.zeromq.Connect
import akka.zeromq.SocketType
import akka.zeromq.zeromqSystem

object Client {
  private val logger = LoggerFactory.getLogger(getClass)
  private val system = ActorSystem("apnclient")
  def apply() = {
    val actor = system.newSocket(SocketType.Req, Connect(Server.conf.getString("apnserver.address")))
    actor
  }
}
