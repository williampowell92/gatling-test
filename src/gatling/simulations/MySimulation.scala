import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

class MySimulation extends Simulation {
  val baseUrl: String = System.getProperty("url", "http://localhost:9005")
  val basePath: String = System.getProperty("path", "src/gatling/")
  val tps: Int = System.getProperty("tps", "2").toInt

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")

  val TPNBList: List[String] = readFile(basePath + "resources/tpnbs.csv").drop(1)
  val requestFeeder: BatchableFeederBuilder[String]#F = separatedValues("requests.csv", '|').batch.queue
  val TPNCList: List[String] = readFile(basePath + "resources/tpncs.csv").drop(1)

  val scn = scenario("PESimulation")
    .feed(requestFeeder)
    .exec { session =>
      val url = session("url").validate[String].toString
      System.out.println(url)

      if (isTPNCRequest(url)) {
        session.set("body", getRandomValues(TPNCList, productsPerRequest()).mkString(","))
      } else {
        session.set("body", getRandomValues(TPNBList, productsPerRequest()).mkString(","))
      }
    }
    .exec(http("Request")
      .post("${url}")
      .body(StringBody("${body}"))
      .check(status.is(200)))

  setUp(
    scn.inject(
      rampUsersPerSec(tps / 10) to tps during (1 minute),
      constantUsersPerSec(tps) during (5 minutes)
    )
  ).protocols(httpProtocol)

  def readFile(filename: String): List[String] = {
    val bufferedSource = Source.fromFile(filename)
    val lines = (for (line <- bufferedSource.getLines()) yield line).toList
    bufferedSource.close
    lines
  }

  private def getRandomValues(array: List[String], count: Int): List[String] = {
    Stream.continually(Random.nextInt(array.size))
      .take(count)
      .map(array(_))
      .toList
  }

  private def productsPerRequest(): Int = {
    val percentage = Random.nextInt(100)

    if (percentage <= 18) { // 18% 1-50
      1 + Random.nextInt(49)
    } else if (percentage <= 30) { // 12% 51-100
      51 + Random.nextInt(49)
    } else if (percentage <= 46) { // 16% 101-200
      101 + Random.nextInt(99)
    } else if (percentage <= 58) { // 12% 201-300
      201 + Random.nextInt(99)
    } else if (percentage <= 67) { // 9% 301-400
      301 + Random.nextInt(99)
    } else if (percentage <= 75) { // 8% 401-500
      401 + Random.nextInt(99)
    } else { // 25% 501-600
      501 + Random.nextInt(99)
    }
  }

  private def isTPNCRequest(url: String): Boolean = url contains "identifierType=TPNC"
}
