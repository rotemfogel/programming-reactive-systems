package kvstore

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive

import scala.collection.mutable

object Arbiter {

  /**
    * This message contains all replicas currently known to the arbiter, including the primary.
    */
  case class Replicas(replicas: Set[ActorRef])
  case object Join
  case object JoinedPrimary
  case object JoinedSecondary

}

class Arbiter extends Actor with ActorLogging {

  import Arbiter._

  var leader: Option[ActorRef] = None
  val replicas = mutable.Set.empty[ActorRef]

  override def receive: Receive = LoggingReceive {
    case Join =>
      if (leader.isEmpty) {
        log.debug("{} joined as primary", sender)
        leader = Some(sender)
        replicas += sender
        sender ! JoinedPrimary
      } else {
        /*replicas += context.actorOf(Unreliable.props(sender))
        sender ! JoinedSecondary*/
        replicas += sender
        sender ! JoinedSecondary
      }
      leader foreach (_ ! Replicas(replicas.toSet))
  }
}