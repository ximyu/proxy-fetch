package edu.arizona.ai

import org.specs._
import org.specs.runner.{ConsoleRunner, JUnit4}
import java.util.Date
import edu.arizona.ai.proxyfetch.Utility

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

class ConnectorSpecTest extends JUnit4(ConnectorSpec)

object ConnectorSpecRunner extends ConsoleRunner(ConnectorSpec)

object ConnectorSpec extends Specification {
  "Initializing the database" should {
    "result in an empty table" in {
      Connector.initDBTable
      Connector.getStoredProxies.size must beEqualTo(0)
    }
  }

  "After inserting 3 records to db, there" should {
    "be 3 records in the table" in {
      val p1 = new Proxy("239.201.20.34", 8080, 3.234, new Date())
      val p2 = new Proxy("239.201.202.134", 8888, 12.434, new Date())
      val p3 = new Proxy("239.201.12.234", 80, 1.34, new Date())
      Connector.storeProxy(p1) must beTrue
      Connector.storeProxy(p2) must beTrue
      Connector.storeProxy(p3) must beTrue
      Connector.getStoredProxies.size must beEqualTo(3)
    }
  }

  "Inserting duplicated record" should {
    "be banned" in {
      val p1 = new Proxy("239.201.20.34", 8080, 3.234, new Date())
      Connector.storeProxy(p1) must beFalse
      Connector.getStoredProxies.size must beEqualTo(3)
    }
  }

  "After deleting 1 record from db, there" should {
    "be 2 records in the table" in {
      Connector.deleteProxy("239.201.20.34", 8080) must beTrue
      Connector.getStoredProxies.size must beEqualTo(2)
    }
  }

  "Updating response time of proxy" should {
    "be persisted in db" in {
      val p1 = new Proxy("239.201.20.34", 8080, 1.26, new Date())
      Connector.updateProxy(p1) must beTrue
      Connector.getStoredProxies.filter(p => p.server == "239.201.20.34" && p.port == 8080).forall(p => p.respTime == 1.26) must beTrue
    }
  }

  "Clearing all proxies records" should {
    "result in an empty table" in {
      Connector.clearAllProxies must beTrue
      Connector.getStoredProxies.size must beEqualTo(0)
    }
  }

  "Testing proxy 24.25.26.82:80" should {
    "just do the testing" in {
      Utility.testProxy(new Proxy("24.25.26.82", 80, Int.MaxValue, new Date)) must throwAn[Exception]
    }
  }
}