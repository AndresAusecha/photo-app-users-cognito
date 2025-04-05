package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CreateUserHandlerTest {
  @Mock
  CognitoUserService cognitoUserService;

  @Mock
  APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

  @Mock
  Context context;

  @Mock
  LambdaLogger logger;

  @InjectMocks
  CreateUserHandler handler;

  @BeforeEach
  public void runBeforeEachTestMethod() {
    System.out.println("Executing @BeforeEach test");

  }

  @AfterEach
  public void runAfterEachTestMethod() {
    System.out.println("Executing @AfterEach test");
  }

  @Test
  public void testHandleRequest_whenValidDetailsProvided_returnsSuccessfulResponse() {
    // Arrange or given
    JsonObject userDetails = new JsonObject();
    userDetails.addProperty("firstName", "Andres");
    userDetails.addProperty("lastName", "Ausecha");
    userDetails.addProperty("email", "daausecham@gmail.com");
    userDetails.addProperty("password", "12345");

    String userDetailsAsEmail = new Gson().toJson(userDetails);

    when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsAsEmail);

    when(context.getLogger()).thenReturn(logger);

    JsonObject createUserResult = new JsonObject();
    createUserResult.addProperty("isSuccessful", true);
    createUserResult.addProperty("statusCode", 200);
    createUserResult.addProperty("cognitoUserId", UUID.randomUUID().toString());
    createUserResult.addProperty("isConfirmed", false);

    when(cognitoUserService.createUser(any(), any(), any(), any())).thenReturn(createUserResult);

    // act or when
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    // assert or then
    verify(logger, times(2)).log(anyString());
    assertTrue(responseBodyJson.get("isSuccessful").getAsBoolean());
  }

  @Test
  public void testHandleRequest_whenEmptyBodyProvided_returnErrorResponse() {
    when(apiGatewayProxyRequestEvent.getBody()).thenReturn("");
    when(context.getLogger()).thenReturn(logger);

    // act or when
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    assertEquals(Integer.valueOf(500), responseEvent.getStatusCode());
    assertNotNull(responseBodyJson.get("message"));
  }

  @Test
  public void testHandleRequest_whenAwsServiceExceptionTakesPlace_returnErrorMessage() {
    when(apiGatewayProxyRequestEvent.getBody()).thenReturn("{}");
    when(context.getLogger()).thenReturn(logger);
    AwsErrorDetails awsErrorDetails = AwsErrorDetails
            .builder()
            .errorCode("")
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
            .errorMessage("AwsServiceException took place")
            .build();
    when(cognitoUserService.createUser(any(), any(), any(), any())).thenThrow(
            AwsServiceException
                    .builder()
                    .statusCode(500)
                    .awsErrorDetails(awsErrorDetails)
                    .build()
    );

    // act or when
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    assertEquals(
            Integer.valueOf(awsErrorDetails.sdkHttpResponse().statusCode()),
            responseEvent.getStatusCode()
    );
    assertEquals(awsErrorDetails.errorMessage(), responseBodyJson.get("message").getAsString());
  }
}
