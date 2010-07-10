package edu.arizona.ai

import scala.actors.Actor._
import collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import edu.arizona.ai.proxyfetch.Utility
import java.util.{Properties, Date}
import java.io.{IOException, BufferedReader, InputStreamReader, FileInputStream}

/**
 * @author Ximing Yu
 * @version 0.2, 7/5/2010
 */

object FetchLauncher {
  private val log = LoggerFactory.getLogger(getClass)

  var startTime: Long = 0
  var spideringTime: Long = 0
  val sitesFile = "sites.txt"
  val sites = new ListBuffer[String]
  private var numOfBatches = 5
  val confFile = "config.properties"
  val caller = self

  def main(args: Array[String]) {
    startTime = System.currentTimeMillis
    Connector.initDBTable

    val is = new FileInputStream(ClassLoader.getSystemResource(sitesFile).getFile)
    Utility.convertStreamToString(is).split("\n") foreach {x=> sites += x.trim}

    val starter = actor {
      var count = 0
      loopWhile(count < sites.size) {
        receive {
          case "Finished" => count += 1; log.info("One site spidering finished, count=" + count)
          case _ => log.warn("Unknown message")
        }
        if (count == sites.size) {
          caller ! "Spider Finished"
        }

      }
    }

    val fetcher = new FetcherActor
    fetcher.start
    for (site <- sites) fetcher ! Pair(starter, site)

    receive {
      case "Spider Finished" =>
        spideringTime = System.currentTimeMillis - startTime
        log.info("Spider finished in " + spideringTime / 1000 + " seconds, to start testing")
        log.info(Connector.getStoredProxies.size + " proxies spidered in total.")
        launchTesting
      case _ => log.warn("Unknown message")
    }
  }

  def launchTesting = {
    val confProp = new Properties
    try {
      confProp.load(new FileInputStream(ClassLoader.getSystemResource(confFile).getFile))
      if (confProp.containsKey("num.testing.threads")) {
        numOfBatches = confProp.getProperty("num.testing.threads").toInt
      }
    } catch {
      case e: IOException => log.error("Failed to load configuration file: {}", confFile)
    }

    val testingStartTime = System.currentTimeMillis
    log.info("Proxy testing started...Divided into " + numOfBatches + " batches")
    val testingStarter = actor {
      var count = 0
      loopWhile(count < numOfBatches) {
        receive {
          case "Finished" => count += 1; log.info("One batch of testing finished, count=" + count)
          case _ => log.warn("Unknown message")
        }
        if (count == numOfBatches) {
          log.info("All testing finished")
          log.info("Summary:")
          log.info("Spidering: " + spideringTime / 1000 + " seconds")
          log.info("Testing: " + (System.currentTimeMillis - testingStartTime) / 1000 + " seconds")
          log.info("Total time: " + (System.currentTimeMillis - startTime) / 1000 + " seconds")
        }

      }
    }

    val allProxies = Connector.getStoredProxies
    val batchSize = allProxies.size / numOfBatches + 1
    val testingActor = new TestingActor
    testingActor.start
    (1 to numOfBatches) foreach {i => testingActor ! Pair(testingStarter, allProxies.slice((i - 1) * batchSize, (i * batchSize) min allProxies.size))}
  }

}