package edu.arizona.ai.proxyfetch

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import java.io.{InputStream, BufferedReader, InputStreamReader, IOException}
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.HttpHost
import java.security.MessageDigest
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.http.params.CoreProtocolPNames
import edu.arizona.ai.Connector
import edu.arizona.ai.Proxy
import org.slf4j.LoggerFactory

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

object Utility {

  private val log = LoggerFactory.getLogger(getClass)

  val TestUrl           =   """http://ximyu.dyndns.org/"""

  val ipPortRE          =   """\b(?:\d{1,3}\.){3}\d{1,3}:\d{2,6}\b""".r
  val domainNamePortRE  =   """\b[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}:\d{2,6}\b""".r
  val addressPortRE     =   """(.+?):(.+?)""".r

  val cnproxyAssignRE   =   """(\w)="(\d)";""".r
  val cnproxyRowRE      =   """((?:\d{1,3}\.){3}\d{1,3})<SCRIPT[^>]+>document\.write\(":"(.+?)\)""".r
  val samairAssignRE    =   """(\w)=(\d);""".r
  val samairRowRE       =   """((?:\d{1,3}\.){3}\d{1,3})<script[^>]+>document\.write\(":"(.+?)\)""".r
  val riTableRE        =   """(?is)var x=Math\.round\(Math\.sqrt\((\d+)\).+?hideText\d*\('(.+?)'\)""".r
  val proxyCnIpPortRE   =   """(?is)onDblClick="clip\('((?:\d{1,3}\.){3}\d{1,3}:\d{2,6})'\);alert""".r
  
  val cnproxyeduRE      =   """(.+?cnproxy.+?edu.+?)""".r
  val cnproxyRE         =   """(.+?cnproxy.+?)""".r
  val freshproxyRE      =   """(.+?freshproxy.+?)""".r
  val myproxyRE         =   """(.+?my-proxy.+?)""".r
  val proxylistRE       =   """(.+?proxylist.+?)""".r
  val samairRE          =   """(.+?samair.+?)""".r
  val rosinstrumentRE   =   """(.+?rosinstrument.+?)""".r
  val proxycnRE         =   """(.+?proxycn.+?)""".r

  def testProxy(proxy: Proxy) {
    log.info("Testing proxy {}:{}", proxy.server, proxy.port)
    val beforeTest = System.currentTimeMillis
    var pageContent = ""
    try {
      pageContent = Utility.getWebContentWithProxy(Utility.TestUrl, proxy.server, proxy.port).getOrElse("")
    } catch {
      case e: Exception => log.error("Proxy {}:{} does not work", proxy.server, proxy.port)
    }
    val md5WithProxy = Utility.md5SumString(pageContent.getBytes)
    val span = (System.currentTimeMillis - beforeTest) / 1000
    if (span > 10) {
      Connector.deleteProxy(proxy.server, proxy.port)
      log.warn("Proxy {}:{} will be discarded::response too slow", proxy.server, proxy.port)
      throw new Exception("Proxy not working")
    } else {
      val md5WithoutProxy = Utility.md5SumString(Utility.getWebContent(Utility.TestUrl).getOrElse("").getBytes)
      if (md5WithProxy == md5WithoutProxy) {
        val newProxy = new Proxy(proxy.server, proxy.port, (System.currentTimeMillis - beforeTest) / 1000.0, new Date)
        log.info("Proxy {}:{} responded within required time period.", proxy.server, proxy.port)
        Connector.updateProxy(newProxy)
      } else {
        log.warn("Proxy {}:{} will be discarded::cannot get the test page", proxy.server, proxy.port)
        Connector.deleteProxy(proxy.server, proxy.port)
        throw new Exception("Proxy not working")
      }
    }
  }

  def persistProxyList(proxyList: List[Proxy]) = {
    proxyList foreach {Connector.storeProxy(_)}
  }

  def getWebContent(url: String): Option[String] = {
    val client: HttpClient = new DefaultHttpClient()
    client.getParams.setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/534.1 (KHTML, like Gecko) Chrome/6.0.437.3 Safari/534.1")
    try {
      val entity =  client.execute(new HttpGet(url)).getEntity
      return new Some(convertStreamToString(entity.getContent))
    } catch {
      case e: Exception => println("[ERROR] " + e.getMessage); return new Some("")
    }
  }

  def getWebContentWithProxy(url: String, server: String, port: Int): Option[String] = {
    val client: HttpClient = new DefaultHttpClient()
    client.getParams.setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/534.1 (KHTML, like Gecko) Chrome/6.0.437.3 Safari/534.1")
    client.getParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(server, port))
    try {
      val entity = client.execute(new HttpGet(url)).getEntity
      return new Some(convertStreamToString(entity.getContent))
    } catch {
      case e: Exception => println("[ERROR] " + e.getMessage); return None
    }
  }

  def md5SumString(bytes : Array[Byte]) : String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(bytes)

    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }

  def convertStreamToString(is : InputStream) : String = {
    def inner(reader : BufferedReader, sb : StringBuilder) : String = {
      val line = reader.readLine()
      if(line != null) {
        try {
          inner(reader, sb.append(line + "\n"))
        } catch {
          case e: IOException => e.printStackTrace()
        } finally {
          try {
            is.close()
          } catch {
            case e: IOException => e.printStackTrace()
          }
        }
      }
      sb.toString()
    }

    inner(new BufferedReader(new InputStreamReader(is)), new StringBuilder())
  }

  def unescapeString(text: String, param: Int): String = {
		val pattern = """(?is)(%\w{2})""".r
		var toText = text
    pattern.findAllIn(text).foreach({x => toText = toText.replace(x, decodeHexString(x.substring(1,3)))})
		val sb = new StringBuilder
		for (i <- 0 until toText.length) {
			val newChar = (toText.charAt(i) ^ param).asInstanceOf[Char]
			sb.append(newChar)
		}
		sb.toString()
	}

  def decodeHexString(hexText: String): String = {
    if (hexText.length > 0) {
      val numBytes = hexText.length / 2
      var rawToByte = new Array[Byte](numBytes)
      var offset = 0
      for (i <- 0 until numBytes) {
        val chunk = hexText.substring(offset, offset + 2)
        offset += 2
        rawToByte(i) = (Integer.parseInt(chunk, 16) & 0x000000FF).asInstanceOf[Byte]
      }
      new String(rawToByte)
    } else {""}
  }
}