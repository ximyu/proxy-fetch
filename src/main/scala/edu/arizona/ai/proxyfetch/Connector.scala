package edu.arizona.ai.proxyfetch

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

trait Connector extends Logging {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
  val tableName = PropertyLoader.dbTableName

  System.setProperty("jdbc.drivers", PropertyLoader.dbDriver)

  /**
   * @return The connection object
   */
  def getConnection: Connection = {
    DriverManager.getConnection(PropertyLoader.dbConnUrl, PropertyLoader.dbUsername, PropertyLoader.dbPassword)
	}

  /**
   * @return Whether the table is successfully initialized in db
   */
  def initDBTable: Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("""DROP TABLE IF EXISTS """ + tableName)
      val ps2 = conn.prepareStatement("""CREATE TABLE """ + tableName + """ (
                                        `server` varchar(255),
                                        `port` int(11),
                                        `response` double,
                                        `errortime` int(11) default 0,
                                        `tested` int(1) default 0,
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
   * @return Whether the table is successfully dropped from db
   */
  def dropDBTable: Boolean = {
    val conn = getConnection
    try {
      val ps = conn.prepareStatement("""DROP TABLE IF EXISTS """ + tableName)
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }

  /**
   * Report error for using the proxy. If the proxy has been causing error for
   * over MAXDEFINED times, then it will be deleted
   *
   * @return Whether the error was successfully reported
   */
  def reportProxyError(server: String, port: Int): Boolean = {
    val conn = getConnection
    try {
     val ps1 = conn.prepareStatement("SELECT errortime FROM " + tableName + " WHERE server=? AND port=?")
      ps1.setString(1, server)
      ps1.setInt(2, port)
      val rs = ps1.executeQuery
      if (rs.next) {
        val errorTime = rs.getInt(1) + 1
        if (errorTime < PropertyLoader.errorTimeLimit) {
          val ps2 = conn.prepareStatement("UPDATE " + tableName + " SET errortime=? WHERE server=? AND port=?")
          ps2.setInt(1, errorTime)
          ps2.setString(2, server)
          ps2.setInt(3, port)
          ps2.executeUpdate
        } else {
          deleteProxy(server, port)
        }
      }
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
      val ps = conn.prepareStatement("SELECT server, port, response, errortime, tested FROM " + tableName + " ORDER BY response")
      val rs = ps.executeQuery
      while (rs.next) {
        val proxy = new Proxy(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getInt(4), if (rs.getInt(5) == 1) {true} else {false})
        proxyList += proxy
        log.debug(rs.getString(1) + ":" + rs.getInt(2) + "=>response time: " + rs.getDouble(3) + " seconds")
      }
      proxyList.toList
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); List[Proxy]()
    } finally {
      conn.close
    }
  }

  /**
   * @return The proxy rows stored in the database that are working (e.g. responseTime < limit, errorTime < limit)
   */
  def getGoodProxies: List[Proxy] = {
    val conn = getConnection
    val proxyList = new ListBuffer[Proxy]
    try {
      val ps = conn.prepareStatement("SELECT server, port, response, errortime, tested FROM " + tableName +
              " WHERE response < ? AND errortime < ? AND tested = 1 ORDER BY response")
      ps.setDouble(1, PropertyLoader.responseLimit)
      ps.setInt(2, PropertyLoader.errorTimeLimit)
      val rs = ps.executeQuery
      while (rs.next) {
        val proxy = new Proxy(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getInt(4), true)
        proxyList += proxy
        log.debug(rs.getString(1) + ":" + rs.getInt(2) + "=>response time: " + rs.getDouble(3) + " seconds")
      }
      proxyList.toList
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); List[Proxy]()
    } finally {
      conn.close
    }
  }

  /**
   * @return The proxy rows that haven't been tested. The number of proxies returned id decided by the batch size
   */
  def getUntestedProxiesByBatch: List[Proxy] = {
    val conn = getConnection
    val proxyList = new ListBuffer[Proxy]
    try {
      val ps = conn.prepareStatement("SELECT server, port, response, errortime FROM " + tableName + " WHERE tested = 0 LIMIT 0," + PropertyLoader.testingBatchSize)
      val rs = ps.executeQuery
      while (rs.next) {
        val proxy = new Proxy(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getInt(4), false)
        proxyList += proxy
        log.debug(rs.getString(1) + ":" + rs.getInt(2) + "=>response time: " + rs.getDouble(3) + " seconds")
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
      val ps = conn.prepareStatement("INSERT INTO " + tableName + " (server, port, response) VALUES (?,?,?)")
      ps.setString(1, proxy.server)
      ps.setInt(2, proxy.port)
      ps.setDouble(3, proxy.respTime)
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
      val ps = conn.prepareStatement("UPDATE " + tableName + " SET response=?, errortime=?, tested=? WHERE server=? AND port=?")
      ps.setDouble(1, proxy.respTime)
      ps.setInt(2, proxy.errorTime)
      ps.setInt(3, if (proxy.tested) {1} else {0})
      ps.setString(4, proxy.server)
      ps.setInt(5, proxy.port)
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
      val ps = conn.prepareStatement("DELETE FROM " + tableName + " WHERE server=? AND port=?")
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
      val ps = conn.prepareStatement("DELETE FROM " + tableName)
      ps.executeUpdate
      true
    } catch {
      case e: Exception => log.error("[ERROR] {}", e.getMessage); false
    } finally {
      conn.close
    }
  }
  
}

object TestConnector extends Connector {
  override val tableName = "testproxy"
}