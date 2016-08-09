package com.hextremelabs.ussd.handler;

import com.hextremelabs.ussd.dto.UssdRequest;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sayo Oladeji
 */
public class UssdHandlerTest {

  @Test
  public void invoke() throws Exception {
    UssdHandler handler = new UssdHandler() {
      private String[] doStuff(Map<String, Object> data) {
        return new String[]{"Sayo"};
      }
    };

    assertEquals(1, handler.invoke("doStuff", new HashMap<>()).length);
    assertEquals("Sayo", handler.invoke("doStuff", new HashMap<>())[0]);
  }

  @Test
  public void parseRequest() throws Exception {
    final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getParameter("provider")).thenReturn("mtn");
    when(httpRequest.getParameter("msisdn")).thenReturn("2348031234567");
    when(httpRequest.getParameter("sessionid")).thenReturn("1234567890abcdefg");
    when(httpRequest.getParameter("message")).thenReturn("*123#");

    UssdHandler handler = new UssdHandler() {};
    final UssdRequest request = handler.parseRequest(httpRequest);
    assertEquals("mtn", request.getProvider());
    assertEquals("2348031234567", request.getMsisdn());
    assertEquals("1234567890abcdefg", request.getSessionId());
    assertEquals("*123#", request.getMessage());
  }
}