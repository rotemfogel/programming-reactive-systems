package kvstore

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive

import scala.util.Random

object Persistence {

  def props(flaky: Boolean): Props = Props(new Persistence(flaky))
  case class Persist(key: String, valueOption: Option[String], id: Long)
  case class PersistRetry(key: String, valueOption: Option[String], id: Long)
  case class Persisted(key: String, id: Long)
  class PersistenceException extends Exception("Persistence failure")
}

class Persistence(flaky: Boolean) extends Actor with ActorLogging {

  import Persistence._

  override def receive: Receive = LoggingReceive {
    case Persist(key, _, id) =>
      if (!flaky || Random.nextBoolean()) sender ! Persisted(key, id)
      else throw new PersistenceException
  }
}