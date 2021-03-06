/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package kafka.server

import org.apache.kafka.common.protocol.types.Struct
import org.apache.kafka.common.protocol.{ApiKeys, Errors, ProtoUtils}
import org.apache.kafka.common.requests.ApiVersionsResponse.ApiVersion
import org.apache.kafka.common.requests.{ApiVersionsRequest, ApiVersionsResponse}
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

object ApiVersionsRequestTest {
  def validateApiVersionsResponse(apiVersionsResponse: ApiVersionsResponse) {
    assertEquals("API keys in ApiVersionsResponse must match API keys supported by broker.", ApiKeys.values.length, apiVersionsResponse.apiVersions.size)
    for (expectedApiVersion: ApiVersion <- ApiVersionsResponse.API_VERSIONS_RESPONSE.apiVersions.asScala) {
      val actualApiVersion = apiVersionsResponse.apiVersion(expectedApiVersion.apiKey)
      assertNotNull(s"API key ${actualApiVersion.apiKey} is supported by broker, but not received in ApiVersionsResponse.", actualApiVersion)
      assertEquals("API key must be supported by the broker.", expectedApiVersion.apiKey, actualApiVersion.apiKey)
      assertEquals(s"Received unexpected min version for API key ${actualApiVersion.apiKey}.", expectedApiVersion.minVersion, actualApiVersion.minVersion)
      assertEquals(s"Received unexpected max version for API key ${actualApiVersion.apiKey}.", expectedApiVersion.maxVersion, actualApiVersion.maxVersion)
    }
  }
}

class ApiVersionsRequestTest extends BaseRequestTest {

  override def numBrokers: Int = 1

  @Test
  def testApiVersionsRequest() {
    val apiVersionsResponse = sendApiVersionsRequest(new ApiVersionsRequest.Builder().build())
    ApiVersionsRequestTest.validateApiVersionsResponse(apiVersionsResponse)
  }

  @Test
  def testApiVersionsRequestWithUnsupportedVersion() {
    val apiVersionsRequest = new ApiVersionsRequest(
      new Struct(ProtoUtils.currentRequestSchema(ApiKeys.API_VERSIONS.id)), Short.MaxValue)
    val apiVersionsResponse = sendApiVersionsRequest(apiVersionsRequest)
    assertEquals(Errors.UNSUPPORTED_VERSION, apiVersionsResponse.error)
  }

  private def sendApiVersionsRequest(request: ApiVersionsRequest): ApiVersionsResponse = {
    val response = send(request, ApiKeys.API_VERSIONS)
    ApiVersionsResponse.parse(response)
  }
}
