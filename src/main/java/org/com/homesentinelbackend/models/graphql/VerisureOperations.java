package org.com.homesentinelbackend.models.graphql;

public final class VerisureOperations {

    private VerisureOperations() {}

    public static final String MK_LOGIN_TOKEN = """
        mutation mkLoginToken($user: String!, $password: String!, $id: String!, $country: String!, $lang: String!, $callby: String!) {
          xSLoginToken(
            user: $user
            password: $password
            id: $id
            country: $country
            lang: $lang
            callby: $callby
          ) {
            res
            msg
            hash
            lang
            legals
            changePassword
            needDeviceAuthorization
            __typename
          }
        }
        """;

    public static final String MK_VALIDATE_DEVICE = """
        mutation mkValidateDevice($idDevice: String, $idDeviceIndigitall: String, $uuid: String, $deviceName: String, $deviceBrand: String, $deviceOsVersion: String, $deviceVersion: String, $password: String) {
          xSValidateDevice(
            idDevice: $idDevice
            idDeviceIndigitall: $idDeviceIndigitall
            uuid: $uuid
            deviceName: $deviceName
            deviceBrand: $deviceBrand
            deviceOsVersion: $deviceOsVersion
            deviceVersion: $deviceVersion
            password: $password
          ) {
            res
            msg
            hash
            refreshToken
            legals
            __typename
          }
        }
        """;

    public static final String MK_SEND_OTP = """
        mutation mkSendOTP($recordId: Int!, $otpHash: String!) {
          xSSendOtp(recordId: $recordId, otpHash: $otpHash) {
            res
            msg
            __typename
          }
        }
        """;

    public static final String SRV_STATUS = """
        query Srv($numinst: String!, $uuid: String) {
          xSSrv(numinst: $numinst, uuid: $uuid) {
            res
            msg
            language
            installation {
              numinst
              role
              alias
              status
              panel
              sim
              instIbs
              services {
                idService
                active
                visible
                bde
                isPremium
                codOper
                request
                minWrapperVersion
                unprotectActive
                unprotectDeviceStatus
                instDate
                devices {
                  id
                  code
                  name
                  type
                  __typename
                }
                genericConfig {
                  total
                  attributes {
                    key
                    value
                    __typename
                  }
                  __typename
                }
                attributes {
                  attributes {
                    name
                    value
                    active
                    __typename
                  }
                  __typename
                }
                __typename
              }
              configRepoUser {
                alarmPartitions {
                  id
                  enterStates
                  leaveStates
                  __typename
                }
                __typename
              }
              capabilities
              __typename
            }
            __typename
          }
        }
        """;
}
