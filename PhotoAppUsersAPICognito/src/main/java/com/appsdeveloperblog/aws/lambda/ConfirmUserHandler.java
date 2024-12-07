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

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {


    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public ConfirmUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();
        try {
            String requestBodyJsonString = input.getBody();
            JsonObject body = JsonParser.parseString(requestBodyJsonString).getAsJsonObject();
            JsonObject confirmUserResponse = cognitoUserService.confirmUserSignup(
                    appClientId,
                    appClientSecret,
                    body.get("email").getAsString(),
                    body.get("code").getAsString()
            );
            responseEvent.withBody(confirmUserResponse.toString());
            responseEvent.withStatusCode(200);

        } catch (AwsServiceException e){
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
