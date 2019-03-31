package kvstore

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

object Replicator {

  def props(replica: ActorRef): Props = Props(new Replicator(replica))

  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class ReplicateRetry(key: String, valueOption: Option[String], seq: Long)
  case class Replicated(key: String, id: Long)
  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)
}

class Replicator(val replica: ActorRef) extends Actor with ActorLogging {

  import Replicator._
  import context.dispatcher

  import scala.concurrent.duration._
  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, (ActorRef, Replicate)]
  // a sequence of not-yet-sent snapshots (you can disregard this if not implementing batching)
  var pending = Vector.empty[Snapshot]

  val seqCounter = new AtomicLong(0)

  override def receive: Receive = LoggingReceive {
    case Replicate(key, valueOption, id) =>
      val seq = nextSeq()
      log.info("replicate key {} on {} with seq {}", key, replica, seq)
      replica ! Snapshot(key, valueOption, seq)
      acks += ((seq, (sender, Replicate(key, valueOption, id))))
      context.system.scheduler.scheduleOnce(50.milliseconds) {
        self ! ReplicateRetry(key, valueOption, seq)
      }

    case ReplicateRetry(key, valueOption, seq) if acks.get(seq).nonEmpty =>
      log.debug("re-send snapshot request with seq {}", seq)
      replica ! Snapshot(key, valueOption, seq)
      context.system.scheduler.scheduleOnce(50.milliseconds) {
        self ! ReplicateRetry(key, valueOption, seq)
      }

    case SnapshotAck(_, seq) if acks.get(seq).nonEmpty =>
      for ((primary, Replicate(key, _, id)) <- acks.get(seq)) {
        acks -= seq
        log.debug("operation {} acknowledged, send {} to {}", seq, Replicated(key, id), primary)
        primary ! Replicated(key, id)
      }
  }

  private def nextSeq(): Long = seqCounter.getAndIncrement()
}