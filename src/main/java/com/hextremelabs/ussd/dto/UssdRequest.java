package com.hextremelabs.ussd.dto;

/**
 * Value object representing HTTP request from telco. Request comes in like this:
 * provider=airtel&amp;msisdn=2348024675639&amp;sessionid=14568284374231393&amp;message={@literal *}833{@literal *}400#
 *
 * @author Oluremi Adekanmbi
 * @author Sayo Oladeji
 */
public class UssdRequest {

  private final String provider;
  private final String msisdn;
  private final String sessionId;
  private final String message;


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
