package com.bdd.restsalad.steps;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class RestSalad {

  @Given("^I start building a request with \"([^\"]*)\" method and URL \"([^\"]*)\"$")
  public void i_start_building_a_request_with_method_and_URL(String method, String serviceUrl) throws Throwable {
    RestContext.method = method.toUpperCase();
    RestContext.webresource = Client.create().resource(serviceUrl);
  }

  @Given("^I add \"([^\"]*)\" equal to \"([^\"]*)\" as parameter to request$")
  public void i_add_equal_to_as_parameter_to_request(String key, String value) throws Throwable {
    RestContext.webresource = RestContext.webresource.queryParam(key, value);
  }

  @Given("^I add post body to the request as:$")
  public void i_add_post_body_to_the_request_as(String postBody) throws Throwable {
    RestContext.postBody = postBody;
  }

  @Given("^I add the below values as headers to the request:$")
  public void i_add_the_below_values_as_headers_to_the_request(DataTable headerTable) throws Throwable {
    List<Map<String, String>> headerMap = headerTable.asMaps(String.class, String.class);
    if (headerMap != null && headerMap.size() > 0) {
      if(RestContext.headerMapList!=null){
        RestContext.headerMapList.addAll(headerMap);
      }else{
        RestContext.headerMapList = headerMap;
      }
    }
  }

  @Given("^I retrieve the resource$")
  public void i_retrieve_the_resource() throws Throwable {
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

  @Then("^the status code returned should be (\\d+)$")
  public void the_status_code_returned_should_be(int status) throws Throwable {
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
  public void the_response_should_contain(String achvAttrPathToCheck) throws Throwable {
    List<String> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(achvAttrPathToCheck, new TypeRef<List<String>>() {});
    assertThat(achvAttrValuesActual, iterableWithSize(greaterThanOrEqualTo(1)));
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

  @Then("^The response should contain \"([^\"]*)\" as empty array$")
  public void the_response_should_contain_as_empty_array(String arrayPath) throws Throwable {
    List<Object> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(arrayPath, new TypeRef<List>() {});
    assertThat(achvAttrValuesActual, emptyIterable());
  }

  @Then("^The response should contain \"([^\"]*)\" with (\\d+) elements$")
  public void the_response_should_contain_with_elements(String arrayPath, int arraySize) throws Throwable {
    List<Object> achvAttrValuesActual =
        JsonPath.parse(RestContext.restResponse).read(arrayPath, new TypeRef<List>() {});
    assertThat(achvAttrValuesActual, iterableWithSize(arraySize));
  }

  @Then("^The response is empty$")
  public void the_response_is_empty() throws Throwable {
    assertThat(RestContext.restResponse, isEmptyOrNullString());
  }

}
