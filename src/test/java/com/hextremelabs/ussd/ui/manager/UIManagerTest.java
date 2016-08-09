package com.hextremelabs.ussd.ui.manager;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Sayo Oladeji
 */
public class UIManagerTest {

  private static UIManager uiManager;

  @BeforeClass
  public static void setupClass() throws IOException {
    uiManager = new UIManager();
  }

  @Test
  public void getUIModel() throws Exception {
    assertEquals(4, uiManager.getUIModel().getScreen().size());
  }

  @Test
  public void getHomeScreen() throws Exception {
    assertEquals("Welcome",uiManager.getHomeScreen().getHeader());
  }

  @Test
  public void getScreen() throws Exception {
    assertEquals("Hi %s", uiManager.getScreen("greetings").getBody());
  }

  @Test
  public void getOption() throws Exception {
    assertEquals("Exit", uiManager.getOption(uiManager.getHomeScreen(), (byte) 2).getValue());
  }

  @Test
  public void getAppName() throws Exception {
    assertEquals("TEST", uiManager.getAppName());
  }

  @Test
  public void getErrorMessage() throws Exception {
    assertEquals("Operation failed. Please try again later.", uiManager.getErrorMessage());
  }

  @Test
  public void renderScreen() throws Exception {
    assertEquals("Greetings~Hi Sayo", uiManager.render(uiManager.getScreen("greetings"), "Sayo"));
  }

  @Test
  public void renderString() throws Exception {
    assertEquals("Stuff", uiManager.render("Stuff"));
    assertEquals("Stuff~Stuff", uiManager.render("Stuff\nStuff"));
    final String expected = "aa~sdfsfashajsfdjansdfoijasdfiojasdfiojasfdasdfaisjfsidjfasjlkdfsasdfsfashajsfdjansdfoij" +
        "asdfiojasdfiojasfdasdfaisjfsidjfasjlkdfsasdfsfash...";
    final String data = "aa\nsdfsfashajsfdjansdfoijasdfiojasdfiojasfdasdfaisjfsidjfasjlkdfsasdfsfashajsfdjansdfoij" +
    "asdfiojasdfiojasfdasdfaisjfsidjfasjlkdfsasdfsfashajsfdjansdfoijasdfiojsddf";
    assertEquals(expected, uiManager.render(data));
  }
}