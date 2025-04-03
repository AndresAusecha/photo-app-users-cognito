package com.appsdeveloperblog.aws.lambda;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * Handler for requests to Lambda function.
 */
public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;

    private String appClientId;

    private String appClientSecret;

    public CreateUserHandler(CognitoUserService cognitoUserService, String appClientId, String appClientSecret) {
        this.cognitoUserService = cognitoUserService;
        this.appClientId = appClientId;
        this.appClientSecret = appClientSecret;
    }

    public CreateUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        String requestBody = input.getBody();
        LambdaLogger logger = context.getLogger();
        logger.log("Original json" + requestBody);

        try {
            logger.log("starting to process the body");
            JsonObject parsedBody = JsonParser.parseString(requestBody).getAsJsonObject();

            JsonObject createUserResult = cognitoUserService.createUser(parsedBody, appClientId, appClientSecret, context);
            response.withStatusCode(200);
            String jsonResponseBody = createUserResult.toString();
            response.withBody(jsonResponseBody);
        } catch (AwsServiceException e) {
            logger.log(e.awsErrorDetails().errorMessage());
            response.withStatusCode(500);
            response.withBody("{ " + "\"message\":" + "\"" + e.awsErrorDetails().errorMessage() + "\"" + "}");
        }  catch (IllegalStateException e) {
            logger.log(e.getMessage());
            response.withStatusCode(500);
            response.withBody("{ " + "\"message\":" + "\"" + e.getMessage() + "\"" + "}");
        }



        return response;
    }

}
