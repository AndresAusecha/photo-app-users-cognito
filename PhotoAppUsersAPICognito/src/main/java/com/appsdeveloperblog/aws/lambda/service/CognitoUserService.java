package com.appsdeveloperblog.aws.lambda.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CognitoUserService {
    private final CognitoIdentityProviderClient cognitoIdentityProvider;

    public CognitoUserService(CognitoIdentityProviderClient cognitoIdentityProvider) {
        this.cognitoIdentityProvider = cognitoIdentityProvider;
    }

    public CognitoUserService(String region) {
        this.cognitoIdentityProvider = CognitoIdentityProviderClient.builder().region(Region.of(region)).build();
    }

    public static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ");
        }
    }
    public JsonObject createUser(JsonObject user, String appClientId, String appClientSecret, Context context){
        LambdaLogger logger = context.getLogger();
        String email = user.get("email").getAsString();
        String firstName = user.get("firstName").getAsString();
        String lastName = user.get("lastName").getAsString();
        String userId = UUID.randomUUID().toString();

        AttributeType attributeUserId = AttributeType.builder().name("custom:userId")
                .value(userId).build();

        AttributeType emailAttribute = AttributeType.builder().name("email")
                .value(email).build();

        AttributeType nameAttribute = AttributeType.builder().name("name")
                .value(firstName + " " + lastName).build();

        logger.log("name: " + firstName + " " + lastName);
        logger.log("email: " + email);

        List<AttributeType> attributeTypeList = new ArrayList<>();
        attributeTypeList.add(emailAttribute);
        attributeTypeList.add(nameAttribute);
        attributeTypeList.add(attributeUserId);

        String secretHash = calculateSecretHash(appClientId, appClientSecret, email);

        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username(email)
                .password(user.get("password").getAsString())
                .userAttributes(attributeTypeList)
                .clientId(appClientId)
                .secretHash(secretHash)
                .build();
        SignUpResponse response =  this.cognitoIdentityProvider.signUp(signUpRequest);
        logger.log("sent signup request");

        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty("isSuccessful", response.sdkHttpResponse().isSuccessful());
        createUserResult.addProperty("statusCode", response.sdkHttpResponse().statusCode());
        createUserResult.addProperty("cognitoUserId", response.userSub());
        createUserResult.addProperty("isConfirmed", response.userConfirmed());

        return createUserResult;
    }

    public JsonObject confirmUserSignup(
            String appClientId,
            String appClientSecret,
            String email,
            String confirmationCode
    ){
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .secretHash(generatedSecretHash)
                .username(email)
                .confirmationCode(confirmationCode)
                .clientId(appClientId)
                .build();

        ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProvider.confirmSignUp(confirmSignUpRequest);
        JsonObject confirmUserResponse = new JsonObject();
        confirmUserResponse.addProperty("isSuccessful", confirmSignUpResponse.sdkHttpResponse().isSuccessful());
        confirmUserResponse.addProperty("statusCode", confirmSignUpResponse.sdkHttpResponse().statusCode());

        return confirmUserResponse;

    }
}
