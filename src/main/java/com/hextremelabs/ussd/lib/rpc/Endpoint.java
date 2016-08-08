package com.hextremelabs.ussd.lib.rpc;

import com.hextremelabs.ussd.lib.dto.UssdRequest;
import com.hextremelabs.ussd.lib.exception.InvocationException;
import com.hextremelabs.ussd.lib.exception.UINavigationException;
import com.hextremelabs.ussd.lib.exception.ValidationException;
import com.hextremelabs.ussd.lib.handler.UssdHandler;
import com.hextremelabs.ussd.lib.internal.Internal.Log;
import com.hextremelabs.ussd.lib.session.Session;
import com.hextremelabs.ussd.lib.session.SessionManager;
import com.hextremelabs.ussd.lib.ui.manager.UIManager;
import com.hextremelabs.ussd.lib.ui.model.Option;
import com.hextremelabs.ussd.lib.ui.model.Screen;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static com.hextremelabs.ussd.lib.dto.UssdRequest.fromParameters;
import static com.hextremelabs.ussd.lib.internal.Internal.DataHelper.hasBlank;
import static com.hextremelabs.ussd.lib.ui.model.ScreenType.DISPLAY;
import static com.hextremelabs.ussd.lib.ui.model.Validation.FREE;

/**
 * JAX-RS endpoint for receiving and processing HTTP requests (SMS/USSD) from telco. The request is assumed to come with
 * parameters {@code sessionid}, {@code msisdn}, {@code provider} and {@code message}.
 *
 * This endpoint becomes available at {@code http(s)://server_address:port/context_root/jax-rs_root/endpoint}
 *
 * @author Sayo Oladeji
 */
@Path("endpoint")
public class Endpoint {

  @Inject
  @Named("errorMessage")
  private String errorMessage;

  @Inject
  private UssdHandler handler;

  @Inject
  private SessionManager sessionManager;

  @Inject
  private UIManager uiManager;

  @Log
  @GET
  @Path("")
  @Produces("text/html;charset=UTF-8")
  public String handleGet(@Context HttpServletRequest request) {
    return handleRequest(request);
  }

  @Log
  @POST
  @Path("")
  @Consumes(MediaType.WILDCARD)
  @Produces("text/html;charset=UTF-8")
  public String handlePost(@Context HttpServletRequest request) {
    return handleRequest(request);
  }

  private String handleRequest(HttpServletRequest httpRequest) {
    UssdRequest request = fromParameters(httpRequest.getParameterMap());
    Session session = sessionManager.getSession(request.getSessionId());
    List<String> commands = parse(request.getMessage());
    if (session == null) {
      session = new Session(request);
      session.pushScreen(uiManager.homeScreen(), uiManager.render(uiManager.homeScreen(), null));
      sessionManager.putSession(session);
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

  private List<String> parse(String message) {
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
      commands.remove(lastIndex);
      commands.add(lastCommand);
    }

    if (message.startsWith("*")) {
      commands.remove(0);
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
        if (!hasBlank(screen.getCallback())) {
          executionResult = handler.invoke(screen.getCallback(), session.getData());
          session.putData(screen.getId() + "$Result", executionResult);
        }
        session.pushScreen(uiManager.getScreen(screen.getNextScreen()));
        break;
    }

    screen = session.peekLastScreen().getKey();
    if (screen.getType() == DISPLAY) {
      sessionManager.invalidate(session);
    }

    return uiManager.render(screen, executionResult);
  }

  private void validate(Screen screen, String command) throws ValidationException {
    if (screen.getValidation() == null || screen.getValidation() == FREE) {
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

    if (!command.matches(regex)) {
      throw new ValidationException(screen.getValidation(), command);
    }
  }
}
