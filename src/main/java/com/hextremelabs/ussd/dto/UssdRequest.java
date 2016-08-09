package com.hextremelabs.ussd.dto;

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


  public UssdRequest(String provider, String msisdn, String sessionId, String message) {
    this.provider = provider;
    this.msisdn = msisdn;
    this.sessionId = sessionId;
    this.message = message;
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
