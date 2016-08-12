package com.hextremelabs.ussd.rpc;

import com.hextremelabs.ussd.dto.UssdRequest;
import com.hextremelabs.ussd.exception.InvocationException;
import com.hextremelabs.ussd.exception.UINavigationException;
import com.hextremelabs.ussd.exception.ValidationException;
import com.hextremelabs.ussd.handler.UssdHandler;
import com.hextremelabs.ussd.internal.Internal;
import com.hextremelabs.ussd.internal.Internal.Log;
import com.hextremelabs.ussd.internal.Internal.Pair;
import com.hextremelabs.ussd.internal.VisibleForTesting;
import com.hextremelabs.ussd.session.Session;
import com.hextremelabs.ussd.session.SessionManager;
import com.hextremelabs.ussd.ui.manager.UIManager;
import com.hextremelabs.ussd.ui.model.Option;
import com.hextremelabs.ussd.ui.model.Screen;
import com.hextremelabs.ussd.ui.model.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hextremelabs.ussd.ui.model.ScreenType.DISPLAY;

/**
 * JAX-RS endpoint for receiving and processing HTTP requests (SMS/USSD) from telco. The request is assumed to come with
 * parameters {@code sessionid}, {@code msisdn}, {@code provider} and {@code message}.
 *
 * This endpoint becomes available at {@code http(s)://{server_address}:{port}/{context_root}/ussd/endpoint}
 *
 * @author Sayo Oladeji
 */
@Path("endpoint")
public class Endpoint {

  private static final Logger L = LoggerFactory.getLogger(Endpoint.class);


  @Inject
  private UssdHandler handler;

  @Inject
  private SessionManager sessionManager;

  @Inject
  private UIManager uiManager;

  @Inject
  @Named("errorMessage")
  private String errorMessage;

  public Endpoint() {
  }

  @Inject
  public Endpoint(UssdHandler handler, SessionManager sessionManager, UIManager uiManager) {
    this.handler = handler;
    this.sessionManager = sessionManager;
    this.uiManager = uiManager;
    this.errorMessage = uiManager.getErrorMessage();
  }

  @Log
  @GET
  @Path("")
  @Produces("text/html;charset=UTF-8")
  public String handleGet(@Context HttpServletRequest request) {
    final UssdRequest ussdRequest = handler.parseRequest(request);
    try {
      return handleRequest(ussdRequest);
    } catch (Exception ex) {
      L.info("Error occured while handling request: {}", ussdRequest, ex);
      return errorMessage;
    }
  }

  @Log
  @POST
  @Path("")
  @Consumes(MediaType.WILDCARD)
  @Produces("text/html;charset=UTF-8")
  public String handlePost(@Context HttpServletRequest request) {
    final UssdRequest ussdRequest = handler.parseRequest(request);
    try {
      return handleRequest(ussdRequest);
    } catch (Exception ex) {
      L.info("Error occured while handling request: {}", ussdRequest, ex);
      return errorMessage;
    }
  }

  @VisibleForTesting
  String handleRequest(UssdRequest request) {
    Session session = sessionManager.getSession(request.getSessionId());
    List<String> commands = parse(request.getMessage());
    if (session == null) {
      session = new Session(request);
      session.pushScreen(uiManager.getHomeScreen(), uiManager.render(uiManager.getHomeScreen()));
      if (commands.size() == 1) {
        if (uiManager.getHomeScreen().getType() != DISPLAY) {
          sessionManager.putSession(session);
        }

        return session.peekLastScreen().getValue();
      }
    }

    if (commands.size() == 1 && "#".equals(commands.get(0))) {
      System.out.println("###: " + session.getNavigation());
      session.pollLastScreen();
      final Pair<Screen, String> lastScreen = session.peekLastScreen();
      if (lastScreen != null) {
        return lastScreen.getValue();
      }

      session.pushScreen(uiManager.getHomeScreen(), uiManager.render(uiManager.getHomeScreen()));
      sessionManager.putSession(session);
      return session.peekLastScreen().getValue();
    }

    if (commands.size() > 1) {
      commands.remove(0);
    }

    String result = null;
    if (commands.isEmpty()) {
      return session.peekLastScreen().getValue();
    } else {
      try {
        for (String command : commands) {
          result = execute(session, command);
        }
      } catch (ValidationException ex) {
        result = ex.getMessage();
      } catch (InvocationException ex) {
        result = errorMessage;
        sessionManager.invalidate(session);
      } catch (UINavigationException ex) {
        result = "Navigation error. Screen " + ex.getMessage() + " not found.";
        sessionManager.invalidate(session);
      }
    }

    return uiManager.render(result);
  }

  @VisibleForTesting
  List<String> parse(String message) {
    if (message.length() == 1) {
      return Collections.singletonList(message);
    }

    List<String> commands = new ArrayList<>();
    for (String e : message.split("\\*")) {
      String command = e.trim();
      if (!command.isEmpty()) {
        commands.add(command);
      }
    }

    final int lastIndex = commands.size() - 1;
    if (commands.get(lastIndex).endsWith("#")) {
      String lastCommand = commands.get(lastIndex).substring(0, commands.get(lastIndex).length() - 1);
      commands.set(lastIndex, lastCommand);
    }

    if (message.startsWith("#")) {
      commands.set(0, commands.get(0).substring(1));
    }

    return commands;
  }

  private String execute(Session session, String command)
      throws ValidationException, InvocationException, UINavigationException {

    Screen screen = session.peekLastScreen().getKey();
    String executionResult[] = null;
    switch (screen.getType()) {
      case OPTIONS:
        Option option = uiManager.getOption(screen, Byte.parseByte(command));
        session.pushScreen(uiManager.getScreen(option.getNextScreen()));
        break;
      case TEXT_INPUT:
        validate(screen, command);
        session.putData(screen.getId(), command);
        if (!Internal.DataHelper.hasBlank(screen.getCallback())) {
          executionResult = handler.invoke(screen.getCallback(), session.getData());
          session.putData(screen.getId() + "$Result", executionResult);
        }
        session.pushScreen(uiManager.getScreen(screen.getNextScreen()));
        break;
    }

    screen = session.peekLastScreen().getKey();
    session.peekLastScreen().setValue(uiManager.render(screen, executionResult));
    if (screen.getType() == DISPLAY) {
      sessionManager.invalidate(session);
    } else {
      sessionManager.putSession(session);
    }

    return session.peekLastScreen().getValue();
  }

  @VisibleForTesting
  void validate(Screen screen, String command) throws ValidationException {
    if (screen.getValidation() == null || screen.getValidation() == Validation.FREE) {
      return;
    }

    String regex = null;
    switch (screen.getValidation()) {
      case REGEX:
        regex = screen.getRegex() == null ? ".*" : screen.getRegex();
        break;
      case NUMERIC:
        regex = "\\d+";
        break;
      case ALPHANUMERIC:
        regex = "[a-zA-Z0-9]+";
        break;
    }

    if (command == null || !command.matches(regex)) {
      throw new ValidationException(screen.getValidation(), command);
    }
  }
}
