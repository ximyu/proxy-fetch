package edu.arizona.ai

import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * User: ximyu
 * Date: Jul 10, 2010
 * Time: 3:36:39 PM
 * To change this template use File | Settings | File Templates.
 */

trait Logging {
  lazy val log = LoggerFactory.getLogger(getClass)
}