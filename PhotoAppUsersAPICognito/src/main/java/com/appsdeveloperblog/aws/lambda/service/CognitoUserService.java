package com.appsdeveloperblog.aws.lambda.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
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

    public JsonObject userLogin(JsonObject loginDetails, String appClientId, String appClientSecret){
        String email = loginDetails.get("email").getAsString();
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
        Map<String, String> params = new HashMap<String,String>() {
            {
                put("USERNAME", email);
                put("PASSWORD", loginDetails.get("password").getAsString());
                put("SECRET_HASH", generatedSecretHash);
            }
        };
        InitiateAuthRequest request = InitiateAuthRequest
                .builder()
                .clientId(appClientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(params)
                .build();
        InitiateAuthResponse response = cognitoIdentityProvider.initiateAuth(request);
        AuthenticationResultType result = response.authenticationResult();

        JsonObject loginUserResult = new JsonObject();

        loginUserResult.addProperty("isSuccessful", response.sdkHttpResponse().isSuccessful());
        loginUserResult.addProperty("statusCode", response.sdkHttpResponse().statusCode());
        loginUserResult.addProperty("idToken", result.idToken());
        loginUserResult.addProperty("accessToKen", result.accessToken());
        loginUserResult.addProperty("refreshToKen", result.refreshToken());


        return loginUserResult;
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

    public JsonObject getUser(String accessToken) {
        GetUserRequest req = GetUserRequest.builder().accessToken(accessToken).build();
        GetUserResponse res = cognitoIdentityProvider.getUser(req);

        JsonObject getUserResult = new JsonObject();

        getUserResult.addProperty("isSuccessful", res.sdkHttpResponse().isSuccessful());
        getUserResult.addProperty("statusCode", res.sdkHttpResponse().statusCode());

        List<AttributeType> userAttributes = res.userAttributes();
        JsonObject userDetails = new JsonObject();
        userAttributes.forEach((attribute) -> {
            userDetails.addProperty(attribute.name(), attribute.value());
        });
        getUserResult.add("user", userDetails);

        return getUserResult;
    }
}
