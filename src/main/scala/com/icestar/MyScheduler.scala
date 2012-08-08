package com.icestar
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem

object MyScheduler {
  private val logger = LoggerFactory.getLogger(getClass)
  
  def apply() = {
  }
}

class MyScheduler private (system: ActorSystem) extends AnyRef {

}