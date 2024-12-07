package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CognitoUserService cognitoUserService;

    private String appClientId;

    private String appClientSecret;

    public LoginUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }


    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent().withHeaders(headers);

        LambdaLogger logger = context.getLogger();
        try {
            JsonObject loginDetails = JsonParser.parseString(input.getBody()).getAsJsonObject();

            JsonObject loginResult = cognitoUserService.userLogin(loginDetails, appClientId, appClientSecret);

            responseEvent.withBody(loginResult.toString());
        } catch (AwsServiceException e) {
            logger.log(e.awsErrorDetails().errorMessage());
            responseEvent.withStatusCode(500);
            responseEvent.withBody(e.awsErrorDetails().errorMessage());
        }


        return responseEvent;
    }

}
