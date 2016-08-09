package com.hextremelabs.ussd.session;

import com.hextremelabs.ussd.dto.UssdRequest;
import com.hextremelabs.ussd.internal.Internal;
import org.junit.Before;
import org.junit.Test;

import static com.hextremelabs.ussd.internal.Internal.AbstractCache.DEFAULT_TENANT;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Sayo Oladeji
 */
public class SessionManagerTest {

  private SessionManager manager;
  private Internal.Cache cache;

  @Before
  public void setup() {
    cache = spy(Internal.MapCache.class);
    manager = new SessionManager("TEST", cache);
  }

  @Test
  public void createAndRetrieveSession() throws Exception {
    UssdRequest request = new UssdRequest("mtn", "2348031234567", "ab1234567890", "*556#");
    final Session session = new Session(request);
    manager.putSession(session);
    assertEquals(session, manager.getSession("ab1234567890"));
  }

  @Test
  public void invalidate() throws Exception {
    UssdRequest request = new UssdRequest("mtn", "2348031234567", "ab1234567890", "*556#");
    final Session session = new Session(request);
    manager.putSession(session);
    assertEquals(session, manager.getSession("ab1234567890"));
    manager.invalidate(session);
    assertNull(manager.getSession("ab1234567890"));
  }

  @Test
  public void invalidateAll() throws Exception {
    manager.invalidateAll();
    verify(cache, times(1)).clearNamespace(DEFAULT_TENANT, "TEST");
  }

  @Test
  public void sessionExists() throws Exception {
    UssdRequest request = new UssdRequest("mtn", "2348031234567", "ab1234567890", "*556#");
    final Session session = new Session(request);
    manager.putSession(session);
    assertTrue(manager.sessionExists("2348031234567"));
    manager.invalidate(session);
    assertFalse(manager.sessionExists("2348031234567"));
  }

  @Test
  public void getExistingSession() throws Exception {
    UssdRequest request = new UssdRequest("mtn", "2348031234567", "ab1234567890", "*556#");
    final Session session = new Session(request);
    manager.putSession(session);
    assertEquals(session, manager.getExistingSession("2348031234567"));
  }
}