package com.hextremelabs.ussd.lib.session;

import static com.hextremelabs.ussd.lib.internal.Internal.AbstractCache.DEFAULT_TENANT;
import com.hextremelabs.ussd.lib.internal.Internal.Cache;
import com.hextremelabs.ussd.lib.internal.Internal.CacheException;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages session lifecycle.
 *
 * @author Oluremi Adekanmbi
 * @author Sayo Oladeji
 */
@ApplicationScoped
public class SessionManager {

  private static final int FIVE_MINUTES = 5 * 60_000;

  private String reverseQuery;

  @Inject
  @Named("appName")
  private String appName;

  @Inject
  private Cache cache;

  private SessionManager() {
  }

  @PostConstruct
  private void setup() {
    reverseQuery = appName + "_reverse";
  }

  public Session putSession(Session session) throws CacheException {
    cache.put(session.getId(), session, DEFAULT_TENANT, appName, FIVE_MINUTES);
    cache.put(session.getMsisdn(), session, DEFAULT_TENANT, reverseQuery, FIVE_MINUTES);
    return session;
  }

  public Session getSession(String sessionId) {
    Session session = cache.get(sessionId, DEFAULT_TENANT, appName, Session.class);
    return session == null ? null : putSession(session); // Refresh the session for another 5 minutes.
  }

  public void invalidate(Session session) {
    cache.remove(session.getId(), DEFAULT_TENANT, appName);
    cache.remove(session.getMsisdn(), DEFAULT_TENANT, reverseQuery);
  }

  public void invalidateAll() {
    cache.clearNamespace(DEFAULT_TENANT, appName);
    cache.clearNamespace(DEFAULT_TENANT, reverseQuery);
  }

  public boolean sessionExists(String msisdn) {
    return getExistingSessionId(msisdn) != null;
  }

  public Session getExistingSession(String msisdn) {
    return cache.get(getExistingSessionId(msisdn), DEFAULT_TENANT, appName, Session.class);
  }

  private String getExistingSessionId(String msisdn) {
    return cache.get(msisdn, DEFAULT_TENANT, reverseQuery, String.class);
  }
}
