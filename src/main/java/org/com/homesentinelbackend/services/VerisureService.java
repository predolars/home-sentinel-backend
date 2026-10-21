package org.com.homesentinelbackend.services;

import org.com.homesentinelbackend.models.LoginSession;
import org.com.homesentinelbackend.models.auth.VerisurePhone;
import org.com.homesentinelbackend.models.auth.VerisureSecurityHeader;
import org.com.homesentinelbackend.models.graphql.GraphQLRequest;
import org.com.homesentinelbackend.models.graphql.VerisureOperations;
import org.com.homesentinelbackend.models.graphql.variables.LoginTokenVariables;
import org.com.homesentinelbackend.models.graphql.variables.SendOtpVariables;
import org.com.homesentinelbackend.models.graphql.variables.SrvVariables;
import org.com.homesentinelbackend.models.graphql.variables.ValidateDeviceVariables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
public class VerisureService {

    private final GraphQLClient graphQLClient;

    @Value("${credentials.user}")
    private String defaultUser;

    @Value("${credentials.password}")
    private String defaultPassword;

    public VerisureService(GraphQLClient graphQLClient) {
        this.graphQLClient = graphQLClient;
    }

    public LoginSession createSession() {
        return new LoginSession(defaultUser);
    }

    public LoginSession createSession(String user) {
        return new LoginSession(user);
    }

    public JsonNode step1Login(LoginSession session) throws Exception {
        return step1Login(session, defaultUser, defaultPassword);
    }

    public JsonNode step1Login(LoginSession session, String user, String password) throws Exception {
        LoginTokenVariables variables = LoginTokenVariables.of(session.getId(), user, password);
        GraphQLRequest<LoginTokenVariables> request = new GraphQLRequest<>(
                "mkLoginToken",
                variables,
                VerisureOperations.MK_LOGIN_TOKEN
        );
        return graphQLClient.sendRequest(request, session);
    }

    public boolean isMfaRequired(JsonNode step1Response) {
        return step1Response.path("data").path("xSLoginToken").path("needDeviceAuthorization").asBoolean(false)
                || "auth-api.mfa_needed".equals(step1Response.path("data").path("xSLoginToken").path("msg").asText());
    }

    public List<VerisurePhone> extractAvailablePhones(JsonNode step2Response) {
        List<VerisurePhone> phones = new ArrayList<>();
        JsonNode phonesNode = step2Response.path("errors").path(0).path("data").path("auth-phones");
        if (phonesNode.isArray()) {
            for (JsonNode p : phonesNode) {
                phones.add(new VerisurePhone(p.path("id").asInt(), p.path("phone").asText()));
            }
        }
        return phones;
    }

    public JsonNode step2RequestDeviceValidation(LoginSession session) throws Exception {
        return step2RequestDeviceValidation(session, defaultPassword);
    }

    public JsonNode step2RequestDeviceValidation(LoginSession session, String password) throws Exception {
        ValidateDeviceVariables variables = new ValidateDeviceVariables(password);
        GraphQLRequest<ValidateDeviceVariables> request = new GraphQLRequest<>(
                "mkValidateDevice",
                variables,
                VerisureOperations.MK_VALIDATE_DEVICE
        );
        JsonNode response = graphQLClient.sendRequest(request, session);

        JsonNode validateDeviceNode = response.path("data").path("xSValidateDevice");
        if (validateDeviceNode.hasNonNull("hash")) {
            session.setOtpHash(validateDeviceNode.get("hash").asText());
        } else {
            JsonNode errorData = response.path("errors").path(0).path("data");
            if (errorData.hasNonNull("auth-otp-hash")) {
                session.setOtpHash(errorData.get("auth-otp-hash").asText());
            }
        }

        session.setAvailablePhones(extractAvailablePhones(response));

        return response;
    }

    public JsonNode step3SendOtpSms(LoginSession session, int recordId) throws Exception {
        if (session.getOtpHash() == null) {
            throw new IllegalStateException("No se ha encontrado el otpHash en la sesión. Ejecuta el Paso 2 primero.");
        }
        return step3SendOtpSms(session, recordId, session.getOtpHash());
    }

    public JsonNode step3SendOtpSms(LoginSession session, int recordId, String otpHash) throws Exception {
        SendOtpVariables variables = new SendOtpVariables(recordId, otpHash);
        GraphQLRequest<SendOtpVariables> request = new GraphQLRequest<>(
                "mkSendOTP",
                variables,
                VerisureOperations.MK_SEND_OTP
        );
        return graphQLClient.sendRequest(request, session);
    }

    public JsonNode step4ValidateOtp(LoginSession session, String otpToken) throws Exception {
        if (session.getOtpHash() == null) {
            throw new IllegalStateException("No se dispone de otpHash en la sesión para completar la validación.");
        }
        return step4ValidateOtp(session, defaultPassword, otpToken, session.getOtpHash());
    }

    public JsonNode step4ValidateOtp(LoginSession session, String password, String otpToken, String otpHash) throws Exception {
        ValidateDeviceVariables variables = new ValidateDeviceVariables(password);
        GraphQLRequest<ValidateDeviceVariables> request = new GraphQLRequest<>(
                "mkValidateDevice",
                variables,
                VerisureOperations.MK_VALIDATE_DEVICE
        );
        VerisureSecurityHeader securityHeader = VerisureSecurityHeader.forOtp(otpToken, otpHash);

        JsonNode response = graphQLClient.sendRequest(request, session, securityHeader);

        JsonNode validateDeviceNode = response.path("data").path("xSValidateDevice");
        if (validateDeviceNode.hasNonNull("hash")) {
            session.setHash(validateDeviceNode.get("hash").asText());
        }
        if (validateDeviceNode.hasNonNull("refreshToken")) {
            session.setRefreshToken(validateDeviceNode.get("refreshToken").asText());
        }

        return response;
    }

    public JsonNode step5GetInstallationStatus(LoginSession session, String numinst) throws Exception {
        SrvVariables variables = new SrvVariables(numinst);
        GraphQLRequest<SrvVariables> request = new GraphQLRequest<>(
                "Srv",
                variables,
                VerisureOperations.SRV_STATUS
        );
        return graphQLClient.sendRequest(request, session);
    }
}
