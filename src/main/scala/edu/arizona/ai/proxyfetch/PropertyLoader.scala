package edu.arizona.ai.proxyfetch

import java.util.Properties
import java.io.{File, FileNotFoundException, IOException, FileInputStream}

/**
 * Created by IntelliJ IDEA.
 * User: ximyu
 * Date: Jul 10, 2010
 * Time: 3:32:59 PM
 * To change this template use File | Settings | File Templates.
 */

object PropertyLoader extends Logging {
  private val defaultPropFile = "default.properties"
  private val overridePropFile = "proxy-fetch.properties"
  private val prop = new Properties
  try {
    prop.load(new FileInputStream(ClassLoader.getSystemResource(defaultPropFile).getFile))
    val overrideFile = new File(overridePropFile)
    if (overrideFile.exists) {
      prop.load(new FileInputStream(overrideFile))
    }
  } catch {
    case e: IOException => log.error("Failed to load the property file: {}", defaultPropFile)
    case e: FileNotFoundException => log.error("Cannot find the property file: {}", defaultPropFile)
  }
  // Proxy Testing
  val numOfTestingThread = prop.getProperty("proxy.testing.thread.number").toInt
  val testingBatchSize = prop.getProperty("proxy.testing.batch.size").toInt
  val testUrl = prop.getProperty("test.url")
  // The file listing all proxy websites
  val proxySiteListFile = prop.getProperty("proxy.sites.list")
  // The maximum response time allowed for a proxy
  val responseLimit = prop.getProperty("proxy.response.time.limit").toDouble
  val errorTimeLimit = prop.getProperty("proxy.error.time.limit").toInt
  // Database Connection
  val dbConnUrl = prop.getProperty("connection.url")
  val dbDriver = prop.getProperty("jdbc.driver")
  val dbUsername = prop.getProperty("username")
  val dbPassword = prop.getProperty("password")
  val dbTableName = prop.getProperty("table.name")
}