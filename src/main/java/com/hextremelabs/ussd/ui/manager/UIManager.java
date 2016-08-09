/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hextremelabs.ussd.ui.manager;

import com.hextremelabs.ussd.exception.UINavigationException;
import com.hextremelabs.ussd.internal.Internal.Joiner;
import com.hextremelabs.ussd.internal.VisibleForTesting;
import com.hextremelabs.ussd.ui.model.Option;
import com.hextremelabs.ussd.ui.model.Screen;
import com.hextremelabs.ussd.ui.model.UssdApp;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.hextremelabs.ussd.internal.Internal.DataHelper.hasBlank;
import static com.hextremelabs.ussd.internal.Internal.DataHelper.unmarshalXml;

/**
 * Manages the application lifecycle (screens, transitions and rendering). The application object model is loaded from
 * an XML named {@code ussd-app.xml} to be placed in the web application root package.
 *
 * @author Sayo Oladeji
 */
@ApplicationScoped
public class UIManager {

  private final UssdApp uiModel;
  private final Map<String, Screen> screenCache;
  private final Screen home;
  private final String lineSeparator;
  private final String errorMessage;
  private final int maxLength;

  @VisibleForTesting
  public UIManager() throws IOException {
    uiModel = unmarshalXml(UssdApp.class, getClass().getResourceAsStream("/ussd-app.xml"));
    screenCache = new LinkedHashMap<>();
    uiModel.getScreen().forEach(e -> screenCache.put(e.getId(), e));
    home = screenCache.containsKey("home") ? screenCache.get("home") : screenCache.values().iterator().next();
    lineSeparator = hasBlank(uiModel.getLineSeparator()) ? "~" : uiModel.getLineSeparator();
    maxLength = uiModel.getMaxTextLength() == null ? 140 : uiModel.getMaxTextLength();
    errorMessage = hasBlank(uiModel.getErrorMessage()) ? "Operation failed. Please try again later."
        : uiModel.getErrorMessage();
  }

  public UssdApp getUIModel() {
    return uiModel;
  }

  public Screen getHomeScreen() {
    return home;
  }

  public Screen getScreen(String id) throws UINavigationException {
    final Screen result = screenCache.get(id);
    if (result == null) {
      throw new UINavigationException(id);
    }

    return result;
  }

  public Option getOption(Screen screen, byte trigger) {
    for (Option option : screen.getOption()) {
      if (option.getTrigger() == trigger) {
        return option;
      }
    }

    return null;
  }

  @Produces
  @Named("appName")
  public String getAppName() {
    return uiModel.getName();
  }

  @Produces
  @Named("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }

  public String render(Screen screen, String... data) {
    String options = hasBlank(screen.getOption()) ? null : screen.getOption().stream()
        .map(e -> e.getTrigger() + ". " + e.getValue())
        .reduce((e1, e2) -> e1 + lineSeparator + e2).get();

    String template = Joiner.on(lineSeparator).skipNull()
        .join(screen.getHeader(), options, screen.getBody(), screen.getFooter());
    return String.format(template, data);
  }

  public String render(String text) {
    if (text == null) {
      return "";
    }

    String result = text.replace("\n", lineSeparator);
    if (result.length() > maxLength) {
      result = result.substring(0, maxLength - 3) + "...";
    }

    return result;
  }
}
