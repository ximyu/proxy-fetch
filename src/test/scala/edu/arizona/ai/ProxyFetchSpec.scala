package edu.arizona.ai

import org.specs._
import org.specs.runner.{ConsoleRunner, JUnit4}

/**
 * @auhtor Ximing Yu
 * @version 0.2, 7/5/2010
 */

class ProxyFetchSpecTest extends JUnit4(ProxyFetchSpec)

object ProxyFetchSpecRunner extends ConsoleRunner(ProxyFetchSpec)

object ProxyFetchSpec extends Specification {
  "Fetching page 1 of freshproxy.org" should {
    "return more than 5 proxies" in {
      val fetcher = new ProxyFetcher("http://www.freshproxy.org/page/%d", 1, 1)
      fetcher.fetchProxy.size must beGreaterThan(5)
    }
  }

  "Fetching page 1 of proxycn.com" should {
    "return more than 30 proxies" in {
      val fetcher = new ProxyCnProxyFetcher("http://www.proxycn.com/html_proxy/http-%d.html", 1, 1)
      fetcher.fetchProxy.size must beGreaterThan(30)
    }
  }

  "Fetching page 1 of cnproxy.com" should {
    "return more than 30 proxies" in {
      val fetcher = new CnProxyProxyFetcher("http://www.cnproxy.com/proxy%d.html", 1, 1)
      fetcher.fetchProxy.size must beGreaterThan(30)
    }
  }

  "Fetching page 1 of samair.ru" should {
    "return more than 5 proxies" in {
      val fetcher = new SamairProxyFetcher("http://www.samair.ru/proxy/proxy-%02d.htm", 1, 1)
      fetcher.fetchProxy.size must beGreaterThan(5)
    }
  }

  "Fetching http://rosinstrument.com/proxy/" should {
    "return more than 30 proxies" in {
      val fetcher = new RosInstrumentProxyFetcher("http://rosinstrument.com/proxy/")
      fetcher.fetchProxy.size must beGreaterThan(30)
    }
  }
}
