package edu.arizona.ai.proxyfetch

import org.specs._
import org.specs.runner.{ConsoleRunner, JUnit4}
import java.util.Date

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

class ConnectorSpecTest extends JUnit4(ConnectorSpec)

object ConnectorSpecRunner extends ConsoleRunner(ConnectorSpec)

object ConnectorSpec extends Specification {
  import TestConnector._

  "Initializing the database" should {
    "result in an empty table" in {
      initDBTable
      getStoredProxies.size must beEqualTo(0)
    }
  }

  "After inserting 3 records to db, there" should {
    "be 3 records in the table" in {
      val p1 = new Proxy("239.201.20.34", 8080, 3.234)
      val p2 = new Proxy("239.201.202.134", 8888, 12.434)
      val p3 = new Proxy("239.201.12.234", 80, 1.34)
      storeProxy(p1) must beTrue
      storeProxy(p2) must beTrue
      storeProxy(p3) must beTrue
      getStoredProxies.size must beEqualTo(3)
    }
  }

  "Inserting duplicated record" should {
    "be banned" in {
      val p1 = new Proxy("239.201.20.34", 8080, 3.234)
      storeProxy(p1) must beFalse
      getStoredProxies.size must beEqualTo(3)
    }
  }

  "After deleting 1 record from db, there" should {
    "be 2 records in the table" in {
      deleteProxy("239.201.20.34", 8080) must beTrue
      getStoredProxies.size must beEqualTo(2)
    }
  }

  "Updating response time of proxy" should {
    "be persisted in db" in {
      val p1 = new Proxy("239.201.20.34", 8080, 1.26)
      updateProxy(p1) must beTrue
      reportProxyError(p1.server, p1.port)
      getStoredProxies.filter(p => p.server == "239.201.20.34" && p.port == 8080).forall(p => p.respTime == 1.26) must beTrue
      getStoredProxies.filter(p => p.server == "239.201.20.34" && p.port == 8080).forall(p => p.errorTime == 1) must beTrue
    }
  }

  "Clearing all proxies records" should {
    "result in an empty table" in {
      clearAllProxies must beTrue
      getStoredProxies.size must beEqualTo(0)
      dropDBTable must beTrue
    }
  }

  "Testing proxy 24.25.26.82:80" should {
    "just do the testing" in {
      Utility.testProxy(new Proxy("24.25.26.82", 80, Int.MaxValue)) must throwAn[Exception]
    }
  }
}