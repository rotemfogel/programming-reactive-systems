/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import actorbintree.BinaryTreeNode.{CopyFinished, CopyTo}
import actorbintree.BinaryTreeSet._
import akka.actor._

import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {
  import BinaryTreeSet._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case op: Operation => root.forward(op)
    case GC =>
      val newRoot = createRoot
      context.become(garbageCollecting(newRoot))
      root ! CopyTo(newRoot)
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case CopyFinished =>
      pendingQueue.foreach { newRoot ! _ }
      pendingQueue = Queue.empty
      root = newRoot
      context.unbecome()

    case op: Operation =>
      // same as stash()
      pendingQueue = pendingQueue.enqueue(op)

    case GC => /* ignore GC while garbage collection */
  }
}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case Contains(requester, id, e) =>
      if (e == elem && !initiallyRemoved) requester ! ContainsResult(id, !removed)
      else {
        val next = nextPos(e)
        if(subtrees.isDefinedAt(next)) subtrees(next) ! Contains(requester, id, e)
        else requester ! ContainsResult(id, false)
      }
    case Insert(requester, id, e) =>
      if (e == elem && !initiallyRemoved) {
        removed = false
        requester ! OperationFinished(id)
      } else {
        val next = nextPos(e)
        if (subtrees.isDefinedAt(next)) subtrees(next) ! Insert(requester, id, e)
        else {
          subtrees += (next -> context.actorOf(props(e, initiallyRemoved = false)))
          requester ! OperationFinished(id)
        }
      }
    case Remove(requester, id, e) =>
      if (e == elem && !initiallyRemoved) {
        removed = true
        requester ! OperationFinished(id)
      } else {
        val next = nextPos(e: Int)
        if (subtrees.isDefinedAt(next)) subtrees(next) ! Remove(requester, id, e)
        else requester ! OperationFinished(id)
      }
    case CopyTo(treeNode) =>
      val children = subtrees.values.toSet
      if (!removed) treeNode ! Insert(self, -1 /* self insert */ , elem)
      children.foreach {
        _ ! CopyTo(treeNode)
      }
      isCopyDone(children, removed)
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(-1) => isCopyDone(expected, true)
    case CopyFinished => isCopyDone(expected - sender, insertConfirmed)
  }

  private def isCopyDone(expected: Set[ActorRef], insertConfirmed: Boolean): Unit = {
    if(expected.isEmpty && insertConfirmed) self ! PoisonPill
    else context.become(copying(expected, insertConfirmed))
  }

  private def nextPos(e: Int): Position = {
    if (e > elem) Right
    else Left
  }

  override def postStop() = context.parent ! CopyFinished
}
