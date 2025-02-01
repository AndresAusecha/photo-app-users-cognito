package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import java.util.HashMap;
import java.util.Map;

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CognitoUserService cognitoUserService;

    public GetUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        LambdaLogger logger = context.getLogger();
        Map<String, String> requestHeaders = apiGatewayProxyRequestEvent.getHeaders();


        try {
            JsonObject userDetails = cognitoUserService.getUser(requestHeaders.get("AccessToken"));
            responseEvent.withBody(new Gson().toJson(userDetails, JsonObject.class));
            responseEvent.withStatusCode(200);

        } catch (AwsServiceException e) {
            logger.log(e.awsErrorDetails().errorMessage());

            responseEvent.withBody(e.awsErrorDetails().errorMessage());
            responseEvent.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
        } catch (Exception e) {
            logger.log(e.getMessage());

            responseEvent.withBody(e.getMessage());
            responseEvent.withStatusCode(500);
        }

        return responseEvent;
    }
}
