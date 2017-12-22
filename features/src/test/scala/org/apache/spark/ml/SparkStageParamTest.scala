/*
 * Copyright (c) 2017, Salesforce.com, Inc.
 * All rights reserved.
 */

package org.apache.spark.ml

import com.salesforce.op.test.TestSparkContext
import org.apache.hadoop.fs.Path
import org.apache.spark.ml.feature.StandardScaler
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


@RunWith(classOf[JUnitRunner])
class SparkStageParamTest extends FlatSpec with TestSparkContext with BeforeAndAfterEach {
  implicit val formats = DefaultFormats

  var savePath: String = _
  var param: SparkStageParam[StandardScaler] = _
  var stage: StandardScaler = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    savePath = tempDir + "/op-stage-param-test-" + DateTime.now().getMillis
    param = new SparkStageParam[StandardScaler](parent = "test" , name = "test", doc = "none")

    // by setting both to be the same, we guarantee that at least one isn't the default value
    stage = new StandardScaler().setWithMean(false).setWithStd(false)
  }

  // easier if test both at the same time
  "Param" should "encode and decode properly when is set" in {
    param.savePath = Option(savePath)
    val jsonOut = param.jsonEncode(Option(stage))

    // reparse to get encoding value
    val parsed = parse(jsonOut)
    (parsed \ "path").extract[String] shouldBe new Path(savePath, stage.uid).toString
    (parsed \ "className").extract[String] shouldBe stage.getClass.getName

    val stageRecovered = param.jsonDecode(jsonOut).get
    stageRecovered shouldBe a[StandardScaler]
    stageRecovered.getWithMean shouldBe stage.getWithMean
    stageRecovered.getWithStd shouldBe stage.getWithStd
  }

  it should "except out when path is empty" in {
    intercept[RuntimeException](param.jsonEncode(Option(stage)))
  }

  it should "have empty path if stage is empty" in {
    param.savePath = Option(savePath)
    val jsonOut = param.jsonEncode(None)

    val parsed = parse(jsonOut)
    (parsed \ "path").extract[String] shouldBe SparkStageParam.NoPath
    (parsed \ "className").extract[String].isEmpty shouldBe true

    val stageOpt = param.jsonDecode(jsonOut)
    stageOpt.isDefined shouldBe false
  }
}