package com.hextremelabs.ussd.lib.dto;

import java.util.Map;

/**
 * Value object representing HTTP request from telco. Request comes in like this:
 * provider=airtel&msisdn=2348024675639&sessionid=14568284374231393&message=*833*400
 *
 * @author Oluremi Adekanmbi
 * @author Sayo Oladeji
 */
public class UssdRequest {

  private String provider;
  private String msisdn;
  private String sessionId;
  private String message;

  public static UssdRequest fromParameters(Map<String, String[]> parameters) {
    UssdRequest data = new UssdRequest();
    parameters.forEach((key, value) -> data.fill(key, firstOrNull(value)));
    return data;
  }

  private static String firstOrNull(String[] value) {
    return value == null || value.length == 0 ? null : value[0];
  }

  private void fill(String field, String value) {
    switch (field) {
      case "provider":
        this.provider = value;
        break;
      case "msisdn":
        this.msisdn = value;
        break;
      case "sessionid":
        this.sessionId = value;
        break;
      case "message":
        this.message = value;
        break;
    }
  }

  public String getProvider() {
    return provider;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Data{" + "provider=" + provider + ", msisdn=" + msisdn + ", sessionId=" + sessionId + ", message=" + message + '}';
  }
}
