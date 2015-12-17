package com.bdd.restsalad.steps;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

import com.bdd.support.RestConstants;
import com.bdd.support.RestContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource.Builder;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class RestSalad {

  @When("^I request the \"([^\"]*)\" endpoint \"([^\"]*)\"$")
  public void i_request_the_endpoint(String method, String serviceUrl) throws Throwable {
    RestContext.method = method.toUpperCase();
    RestContext.webresource = Client.create().resource(serviceUrl);
  }

  @When("^I provide parameter \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_provide_parameter_as(String key, String value) throws Throwable {
    RestContext.webresource = RestContext.webresource.queryParam(key, value);
  }

  @When("^add postBody as:$")
  public void add_postBody_as(String postBody) throws Throwable {
    RestContext.postBody = postBody;
  }

  @When("^add below headers:$")
  public void add_below_headers(DataTable headerTable) throws Throwable {
    List<Map<String, String>> headerMap = headerTable.asMaps(String.class, String.class);
    if (headerMap != null && headerMap.size() > 0) {
      RestContext.headerMapList = headerMap;
    }
  }

  @When("^I retrieve the JSON results$")
  public void i_retrive_the_JSON_results() throws Throwable {
    System.out.println(
        RestContext.method + " - " + RestContext.webresource.getURI() + ", headers -" + RestContext.headerMapList);

    Builder requestBuilder = RestContext.webresource.getRequestBuilder();
    for (Map<String, String> headerMap : RestContext.headerMapList) {
      requestBuilder =
          requestBuilder.header(headerMap.get(RestConstants.HEADER_NAME), headerMap.get(RestConstants.HEADER_VALUE));
    }
    RestContext.clientResponse = requestBuilder.method(RestContext.method, ClientResponse.class, RestContext.postBody);
    RestContext.restResponse = RestContext.clientResponse.getEntity(String.class);
    RestContext.postBody = null;
    RestContext.headerMapList = new ArrayList<Map<String, String>>();
    System.out.println(RestContext.restResponse);
  }

  @Then("^the status code should be (\\d+)$")
  public void the_status_code_should_be(int status) throws Throwable {
    System.out.println("RestContext.clientResponse = " + RestContext.clientResponse + " status = " + status);
    assertEquals(status, RestContext.clientResponse.getStatus());
  }

  @Then("^The response should contain \"([^\"]*)\" with value \"([^\"]*)\"$")
  public void the_response_should_contain_with_value(String achvAttrPathToCheck, String valueExpected)
      throws Throwable {
    List<String> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(achvAttrPathToCheck, new TypeRef<List<String>>() {});
    for (String achvAttrValueActual : achvAttrValuesActual) {
      assertTrue(achvAttrValueActual.equals(valueExpected));
    }
  }

  @Then("^The response should contain \"([^\"]*)\"$")
  public void the_response_should_contain(String arg1) throws Throwable {

  }

  @Then("^The response should contain \"([^\"]*)\" with values:$")
  public void the_response_should_contain_with_values(String achvAttrPathToCheck, DataTable valueExpected)
      throws Throwable {
    List<String> achvAttrValuesActual = JsonPath.parse(RestContext.restResponse).read(achvAttrPathToCheck);
    List<String> achvAttrValuesExpected = valueExpected.topCells();
    assertThat(achvAttrValuesActual, contains(achvAttrValuesExpected.toArray()));
  }

  @Then("^The \"([^\"]*)\" array has element with below attributes:$")
  public void the_array_has_element_with_below_attributes(String attrPathToCheck, DataTable exptectedTable)
      throws Throwable {
    List<String> attributesNamesToMatch = exptectedTable.topCells();

    List<List<String>> attributesValuesTableToMatch = exptectedTable.cells(1);
    for (List<String> attributeValues : attributesValuesTableToMatch) {
      String attrPathFilterPredicate = attrPathToCheck + "[?(";
      for (String attributeNameToMatch : attributesNamesToMatch) {
        int attrIndex = attributesNamesToMatch.indexOf(attributeNameToMatch);

        if (attrIndex != 0) {
          attrPathFilterPredicate = attrPathFilterPredicate.concat(" && ");
        }
        String attributeValue = StringEscapeUtils.escapeEcmaScript(attributeValues.get(attrIndex));
        attrPathFilterPredicate =
            attrPathFilterPredicate.concat("@." + attributeNameToMatch + "=='" + attributeValue + "'");
      }
      attrPathFilterPredicate = attrPathFilterPredicate.concat(")]");
      assertThat("No elements found for this crtieria- " + attrPathFilterPredicate,
          (List<Object>) JsonPath.parse(RestContext.restResponse).read(attrPathFilterPredicate), iterableWithSize(1));

    }

  }

  @Then("^The response should contain \"([^\"]*)\" is empty array$")
  public void the_response_should_contain_is_empty_array(String arrayPath) throws Throwable {
    List<Object> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(arrayPath, new TypeRef<List>() {});
    assertThat(achvAttrValuesActual, emptyIterable());
  }

  @Then("^The response should contain \"([^\"]*)\" contains (\\d+) elements$")
  public void the_response_should_contain_contains_elements(String arrayPath, int arraySize) throws Throwable {
    List<Object> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(arrayPath, new TypeRef<List>() {});
    assertThat(achvAttrValuesActual, iterableWithSize(arraySize));
  }

  @Then("^The response is empty$")
  public void the_response_is_empty() throws Throwable {
    assertThat(RestContext.restResponse, isEmptyOrNullString());
  }

}