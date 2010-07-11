package edu.arizona.ai.proxyfetch

import scala.actors.Actor._
import actors.{TIMEOUT, Actor}
import org.slf4j.LoggerFactory


/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

class TestingActor extends Actor {
  private val log = LoggerFactory.getLogger(getClass)
  var toContinue = true

  def act() = {
    loopWhile(toContinue) {
      reactWithin(5000) {
        case (caller: Actor, proxies: List[Proxy]) => actor {
          println(Thread.currentThread + " Start testing " + proxies.size + " proxies")
          testProxies(proxies)
          println(Thread.currentThread + " Finished testing " + proxies.size + " proxies")
          caller ! "Finished"
        }

        case TIMEOUT => toContinue = false
      }
    }
  }

  def testProxies(proxies: List[Proxy]) = {
    proxies foreach {x =>
      try {
        Utility.testProxy(x)
      } catch {
        case e: Exception => log.debug("Simply one more proxy discarded....")
      }
    }
  }
}