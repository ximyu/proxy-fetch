package edu.arizona.ai

import org.slf4j.LoggerFactory
import java.sql.{DriverManager, Connection}
import collection.mutable.ListBuffer
import java.text.SimpleDateFormat
import java.util.{Date, Properties}
import java.io.{File, IOException, FileInputStream}

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

object Connector {
  private val log = LoggerFactory.getLogger(getClass)
  private val dbConfig = "db.properties"
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

  private val connProp = new Properties
  try {
    connProp.load(new FileInputStream(ClassLoader.getSystemResource(dbConfig).getFile))
  } catch {
    case e: IOException => log.error("Failed to load db configuration file: {}", dbConfig)
  }
  System.setProperty("jdbc.drivers", connProp.getProperty("JDBC_DRIVER"))
  private val url = connProp.getProperty("CONNECTION_URL")
  private val username = connProp.getProperty("USERNAME")
  private val password = connProp.getProperty("PASSWORD")

  def getConnection: Connection = {
    DriverManager.getConnection(url, username, password)
	}


  /**
   * @return Whether the table is successfully initialized in db
   */
  def initDBTable: Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("""DROP TABLE IF EXISTS proxy;""")
      val ps2 = conn.prepareStatement("""CREATE TABLE proxy (
                                        `server` varchar(255),
                                        `port` int(11),
                                        `response` double,
                                        `updatetime` timestamp,
                                        PRIMARY KEY (`server`,`port`))""")
      ps.executeUpdate
      ps2.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }

  /**
   * @return The proxy rows stored in the database
   */
  def getStoredProxies: List[Proxy] = {
    val conn = getConnection
    val proxyList = new ListBuffer[Proxy]
    try {
      val ps = conn.prepareStatement("SELECT server, port, response, updatetime FROM proxy ORDER BY response")
      val rs = ps.executeQuery
      while (rs.next) {
        val proxy = new Proxy(rs.getString(1), rs.getInt(2), rs.getDouble(3), dateFormat.parse(rs.getString(4)))
        proxyList += proxy
        log.debug(rs.getString(1) + ":" + rs.getInt(2) + "=>response time: " + rs.getDouble(3) + " seconds, tested at: " + rs.getString(4))
      }
      proxyList.toList
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); List[Proxy]()
    } finally {
      conn.close
    }
  }

  /**
   * @param proxy The proxy object to be stored in the database
   * @return Whether the proxy object is successfully stored
   */
  def storeProxy(proxy: Proxy): Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("INSERT INTO proxy (server, port, response, updatetime) VALUES (?,?,?,?)")
      ps.setString(1, proxy.server)
      ps.setInt(2, proxy.port)
      ps.setDouble(3, proxy.respTime)
      ps.setString(4, dateFormat.format(proxy.testTime))
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }

  /**
   * @param proxy The proxy object to be updated in the database
   * @return Whether the proxy object is successfully updated
   */
  def updateProxy(proxy: Proxy): Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("UPDATE proxy SET response=?, updatetime=? WHERE server=? AND port=?")
      ps.setDouble(1, proxy.respTime)
      ps.setString(2, dateFormat.format(new Date))
      ps.setString(3, proxy.server)
      ps.setInt(4, proxy.port)
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }


  /**
   * @param server
   * @param port
   * @return Whether the proxy record is successfully deleted from db
   */
  def deleteProxy(server: String, port: Int): Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("DELETE FROM proxy WHERE server=? AND port=?")
      ps.setString(1, server)
      ps.setInt(2, port)
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }

  /**
   * @return Whether all proxy records are successfully deleted from db
   */
  def clearAllProxies: Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("DELETE FROM proxy")
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }
  
}