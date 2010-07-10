package edu.arizona.ai

import java.util.Date
import edu.arizona.ai.proxyfetch.Utility
import org.slf4j.{Logger, LoggerFactory}

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

case class Proxy (server: String, port: Int, respTime: Double, testTime: Date)

sealed class ProxyFetcher (val urlTemplate: String, val startPage: Int, val endPage: Int) {
  private val log = LoggerFactory.getLogger(getClass)
  protected var proxyList = List[String]()

  def fetchProxy: List[Proxy] = {
    for (i <- startPage to endPage) {
      val url = format(urlTemplate, i)
      log.info("Processing URL: {}", url)
      val pageContent = Utility.getWebContent(url).getOrElse("")
      proxyList :::= Utility.ipPortRE.findAllIn(pageContent).toList
      proxyList :::= Utility.domainNamePortRE.findAllIn(pageContent).toList
    }
    proxyList map {toProxy(_)}
  }

  def toProxy(str: String): Proxy = {
    log.debug(str)
    val Utility.addressPortRE(server, port) = str
    new Proxy(server, port.toInt, Int.MaxValue, new Date())
  }
}

class ProxyCnProxyFetcher(urlTemplate: String, startPage: Int, endPage: Int)
        extends ProxyFetcher(urlTemplate, startPage, endPage) {
  private val log = LoggerFactory.getLogger(getClass)
  
  override def fetchProxy: List[Proxy] = {
    for (i <- startPage to endPage) {
      val url = format(urlTemplate, i)
      log.info("Processing URL: {}", url)
      val pageContent = Utility.getWebContent(url).getOrElse("")
//      log.info(pageContent)
      log.debug("Found {} matches in page", Utility.proxyCnIpPortRE.findAllIn(pageContent).size)
      proxyList :::= Utility.proxyCnIpPortRE.findAllIn(pageContent).matchData.map(_.group(1)).toList
    }
    proxyList map {toProxy(_)}
  }
}

class CnProxyProxyFetcher(urlTemplate: String, startPage: Int, endPage: Int)
        extends ProxyFetcher(urlTemplate, startPage, endPage) {
  private val log = LoggerFactory.getLogger(getClass)

  override def fetchProxy: List[Proxy] = {
    for (i <- startPage to endPage) {
      val url = format(urlTemplate, i)
      log.info("Processing URL: {}", url)
      val pageContent = Utility.getWebContent(url).getOrElse("")
      val assignMap= Utility.cnproxyAssignRE.findAllIn(pageContent).matchData.map(x=>(x.group(1), x.group(2))).toMap
      log.debug("Found {} matches in page {}", Utility.cnproxyRowRE.findAllIn(pageContent).size, url)
      proxyList :::= Utility.cnproxyRowRE.findAllIn(pageContent).matchData.map(x => x.group(1) + ":" + x.group(2).replace("+", "").map(y => assignMap.get(y.toString).get).mkString).toList
    }
    proxyList map {toProxy(_)}
  }
}

class SamairProxyFetcher(urlTemplate: String, startPage: Int, endPage: Int)
        extends ProxyFetcher(urlTemplate, startPage, endPage) {
  private val log = LoggerFactory.getLogger(getClass)

  override def fetchProxy: List[Proxy] = {
    for (i <- startPage to endPage) {
      val url = format(urlTemplate, i)
      log.info("Processing URL: {}", url)
      val pageContent = Utility.getWebContent(url).getOrElse("")
      val assignMap= Utility.samairAssignRE.findAllIn(pageContent).matchData.map(x=>(x.group(1), x.group(2))).toMap
      log.debug("Found {} matches in page {}", Utility.samairRowRE.findAllIn(pageContent).size, url)
      proxyList :::= Utility.samairRowRE.findAllIn(pageContent).matchData.map({x=>
        val ip = x.group(1)
        val port = x.group(2).replace("+", "").map(y => assignMap.get(y.toString).get).mkString
        ip + ":" + port
      }).toList
    }
    proxyList map {toProxy(_)}
  }
}

class RosInstrumentProxyFetcher(url: String) extends ProxyFetcher(url, 0, 0) {
  private val log = LoggerFactory.getLogger(getClass)

  import scala.math._
  override def fetchProxy: List[Proxy] = {
    log.info("Processing URL: {}", url)
    val pageContent = Utility.getWebContent(url).getOrElse("")
    val segmentMatchIt = Utility.riTableRE.findAllIn(pageContent).matchData foreach {x=>
      val exp = x.group(1)
      val unescapedText = Utility.unescapeString(x.group(2), sqrt(exp.toInt).toInt)
      proxyList :::= Utility.ipPortRE.findAllIn(unescapedText).toSet.toList
      proxyList :::= Utility.domainNamePortRE.findAllIn(unescapedText).toSet.toList
    }
    proxyList map {toProxy(_)}
  }
}