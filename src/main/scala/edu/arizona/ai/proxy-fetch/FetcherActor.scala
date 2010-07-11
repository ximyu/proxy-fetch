package edu.arizona.ai

import scala.actors.Actor._
import edu.arizona.ai.proxyfetch.Utility
import actors.{TIMEOUT, Actor}
import util.Random

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */
class FetcherActor extends Actor {

  var toContinue = true

  def act() = {
    loopWhile(toContinue) {
      reactWithin(5000) {
        case (caller: Actor, url: String) => actor {
          val startTime = System.currentTimeMillis
          println(Thread.currentThread + " Spidering " + url);
          fetchSite(url)
          println(Thread.currentThread + " finished spidering in " + (System.currentTimeMillis - startTime) / 1000 + " seconds.")
          caller ! "Finished"
        }

        case TIMEOUT => toContinue = false
      }
    }
  }

  def fetchSite(url: String) = {
    url match {
      case Utility.cnproxyeduRE(cnProxyEdu) =>
        Utility.persistProxyList((new CnProxyProxyFetcher(cnProxyEdu, 1, 2)).fetchProxy)
      case Utility.cnproxyRE(cnProxy) => 
        Utility.persistProxyList((new CnProxyProxyFetcher(cnProxy, 1, 10)).fetchProxy)
      case Utility.freshproxyRE(freshProxy) =>
        Utility.persistProxyList((new ProxyFetcher(freshProxy, 1, 30)).fetchProxy)
      case Utility.myproxyRE(myProxy) =>
        Utility.persistProxyList((new ProxyFetcher(myProxy, 1, 10)).fetchProxy)
      case Utility.proxylistRE(proxyList) =>
        Utility.persistProxyList((new ProxyFetcher(proxyList, 0, 30)).fetchProxy)
      case Utility.samairRE(samair) =>
        Utility.persistProxyList((new SamairProxyFetcher(samair, 1, 10)).fetchProxy) // similar to cnproxy
      case Utility.rosinstrumentRE(ri) =>
        Utility.persistProxyList((new RosInstrumentProxyFetcher(ri)).fetchProxy)
      case Utility.proxycnRE(proxyCn) =>
        Utility.persistProxyList((new ProxyCnProxyFetcher(proxyCn, 1, 10)).fetchProxy)
      case s: String =>
        Utility.persistProxyList((new ProxyFetcher(url, 1, 1)).fetchProxy)
    }
  }
}