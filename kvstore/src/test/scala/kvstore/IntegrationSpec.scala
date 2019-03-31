/**
  * Copyright (C) 2013-2015 Typesafe Inc. <http://www.typesafe.com>
  */
package kvstore

import akka.actor.Props
import org.scalatest.{FunSuiteLike, Matchers}

trait IntegrationSpec
  extends FunSuiteLike
    with Matchers {
  this: KVStoreSuite =>

  /*
   * Recommendation: write a test case that verifies proper function of the whole system,
   * then run that with flaky Persistence and/or unreliable communication (injected by
   * using an Arbiter variant that introduces randomly message-dropping forwarder Actors).
   */

  test("Integration-case1") {
    val arbiter = system.actorOf(Props(classOf[Arbiter]), "integration-case1-arbiter")
    val primary = system.actorOf(Replica.props(arbiter, Persistence.props(flaky = true)), "integration-case1-primary")
    val client = session(primary)
    client.setAcked("k1", "v1")
  }

  test("Integration-case2") {
    val arbiter = system.actorOf(Props(classOf[Arbiter]), "integration-case2-arbiter")
    val primary = system.actorOf(Replica.props(arbiter, Persistence.props(flaky = true)), "integration-case2-primary")
    val client = session(primary)
    client.get("k1") === None

    //TODO use kvstore.given.Arbiter instead of Unreliable
    system.actorOf(Unreliable.props(Replica.props(arbiter, Persistence.props(flaky = true))), "integration-case2-unreliable-replica")
    client.setAcked("k1", "v1")
  }
}