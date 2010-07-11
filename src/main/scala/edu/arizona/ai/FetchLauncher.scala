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

object FetchLauncher extends Connector with Logging {
  var startTime: Long = 0
  var spideringTime: Long = 0
  val sites = new ListBuffer[String]
  val caller = self

  def main(args: Array[String]) {
    launchCrawling(doTesting = true)
  }

  def launchCrawling(doTesting: Boolean = false, firstTime: Boolean = false) = {
    startTime = System.currentTimeMillis

    if (firstTime)
      initDBTable

    val is = new FileInputStream(ClassLoader.getSystemResource(PropertyLoader.proxySiteListFile).getFile)
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
        log.info("Spider finished in " + spideringTime / 1000 + " seconds")
        log.info(getStoredProxies.size + " proxies spidered in total.")
        if (doTesting) {
          log.info("To start testing now.")
          launchTesting
        }
      case _ => log.warn("Unknown message")
    }
  }

  def launchTesting = {
    val numOfBatches = PropertyLoader.numOfTestingThread
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

    val allProxies = getUntestedProxiesByBatch
    val batchSize = allProxies.size / numOfBatches + 1
    val testingActor = new TestingActor
    testingActor.start
    (1 to numOfBatches) foreach {i => testingActor ! Pair(testingStarter, allProxies.slice((i - 1) * batchSize, (i * batchSize) min allProxies.size))}
  }

}