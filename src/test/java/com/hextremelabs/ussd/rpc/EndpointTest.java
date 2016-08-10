package com.hextremelabs.ussd.rpc;

import com.hextremelabs.ussd.exception.ValidationException;
import com.hextremelabs.ussd.handler.UssdHandler;
import com.hextremelabs.ussd.internal.Internal.MapCache;
import com.hextremelabs.ussd.session.SessionManager;
import com.hextremelabs.ussd.ui.manager.UIManager;
import com.hextremelabs.ussd.ui.model.Screen;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.hextremelabs.ussd.ui.model.Validation.ALPHANUMERIC;
import static com.hextremelabs.ussd.ui.model.Validation.FREE;
import static com.hextremelabs.ussd.ui.model.Validation.NUMERIC;
import static com.hextremelabs.ussd.ui.model.Validation.REGEX;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sayo Oladeji
 */
public class EndpointTest {

  private Endpoint endpoint;
  private UssdHandler handler;

  @Before
  public void setup() throws IOException {
    handler = new DummyHandler();
    SessionManager sessionManager = new SessionManager("USSD_APP", new MapCache());
    UIManager uiManager = new UIManager();
    endpoint = new Endpoint(handler, sessionManager, uiManager);
  }

  @Test
  public void handleRequest() throws Exception {
    final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getParameter("provider")).thenReturn("mtn");
    when(httpRequest.getParameter("msisdn")).thenReturn("2348031234567");
    when(httpRequest.getParameter("sessionid")).thenReturn("1234567890abcdefg");
    when(httpRequest.getParameter("message")).thenReturn("*123#");

    assertEquals("Welcome~1. Greet Me~2. Exit", endpoint.handleRequest(handler.parseRequest(httpRequest)));

    when(httpRequest.getParameter("message")).thenReturn("1");
    assertEquals("Lib-USSD~What is your name?", endpoint.handleRequest(handler.parseRequest(httpRequest)));
    when(httpRequest.getParameter("message")).thenReturn("Sayo");
    assertEquals("Greetings~Hi Sayo", endpoint.handleRequest(handler.parseRequest(httpRequest)));

    // Greetings page with is a DISPLAY page terminates the session.
    assertEquals("Welcome~1. Greet Me~2. Exit", endpoint.handleRequest(handler.parseRequest(httpRequest)));

    when(httpRequest.getParameter("message")).thenReturn("2");
    assertEquals("Goodbye~Sad to see you go", endpoint.handleRequest(handler.parseRequest(httpRequest)));

    // Goodbye page with is a DISPLAY page terminates the session.
    assertEquals("Welcome~1. Greet Me~2. Exit", endpoint.handleRequest(handler.parseRequest(httpRequest)));
  }

  @Test
  public void parse() throws Exception {
    List<String> res = endpoint.parse("*556#");
    assertThat(asList("556"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("#556#");
    assertThat(asList("556"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("*556*1*2#");
    assertThat(asList("556", "1", "2"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("#556*1*2#");
    assertThat(asList("556", "1", "2"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("#");
    assertThat(asList("#"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("2");
    assertThat(asList("2"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("My command");
    assertThat(asList("My command"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("*556");
    assertThat(asList("556"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("*556*2*");
    assertThat(asList("556", "2"), new ListEqualityMatcher<String>(res));
    res = endpoint.parse("*556*2*3");
    assertThat(asList("556", "2", "3"), new ListEqualityMatcher<String>(res));

  }

  @Test
  public void validate() throws Exception {
    Screen screen = new Screen();
    screen.setValidation(NUMERIC);
    endpoint.validate(screen, "12345");
    try {
      endpoint.validate(screen, "ab123");
      fail("\"ab123\" is not supposed to pass NUMERIC validation but it did.");
    } catch (ValidationException expected) {
    }

    screen.setValidation(ALPHANUMERIC);
    endpoint.validate(screen, "12345");
    endpoint.validate(screen, "abcde");
    endpoint.validate(screen, "123ab");
    try {
      endpoint.validate(screen, "ab123&");
      fail("\"ab123&\" is not supposed to pass ALPHANUMERIC validation but it did.");
    } catch (ValidationException expected) {
    }
    try {
      endpoint.validate(screen, null);
      fail("null is not supposed to pass ALPHANUMERIC validation but it did.");
    } catch (ValidationException expected) {
    }

    screen.setValidation(REGEX);
    screen.setRegex("[0-9]?[a-z]{3}127");
    endpoint.validate(screen, "5abc127");
    endpoint.validate(screen, "abc127");
    try {
      endpoint.validate(screen, "5ab127");
      fail("\"5ab127\" is not supposed to pass REGEX (\"[0-9]?[a-z]{3}127\") validation but it did.");
    } catch (ValidationException expected) {
    }

    screen.setRegex(null);
    screen.setValidation(FREE);
    endpoint.validate(screen, "My random124589,,.)(&");
    endpoint.validate(screen, "");
    endpoint.validate(screen, null);

    screen.setValidation(null);
    endpoint.validate(screen, "My random124589,,.)(&");
    endpoint.validate(screen, "");
    endpoint.validate(screen, null);
  }

  private static class DummyHandler implements UssdHandler {

    public String[] greetMe(Map<String, Object> data) {
      return new String[]{(String) data.get("nameInput")};
    }
  }

  private class ListEqualityMatcher<T> extends ArgumentMatcher<List<T>> {

    List<T> thisList;

    private ListEqualityMatcher(List<T> thisList) {
      this.thisList = thisList;
    }

    @Override
    public boolean matches(Object argument) {
      if (thisList == null && argument == null) {
        return true;
      } else if (thisList == null || argument == null) {
        return false;
      }

      try {
        List<T> argList = (List<T>) argument;
        if (thisList.size() != argList.size()) {
          return false;
        }

        for (int a = 0; a < thisList.size(); a++) {
          if (!thisList.get(a).equals(argList.get(a))) {
            return false;
          }
        }

        return true;
      } catch (ClassCastException ex) {
        return false;
      }
    }
  }
}