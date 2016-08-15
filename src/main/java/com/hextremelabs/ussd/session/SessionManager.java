package com.hextremelabs.ussd.session;

import com.hextremelabs.ussd.internal.Internal.MapCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import static com.hextremelabs.ussd.internal.Internal.AbstractCache.DEFAULT_TENANT;
import static com.hextremelabs.ussd.internal.Internal.Cache;
import static com.hextremelabs.ussd.internal.Internal.CacheException;

/**
 * Manages session lifecycle.
 *
 * @author Oluremi Adekanmbi
 * @author Sayo Oladeji
 */
@ApplicationScoped
public class SessionManager {

  private static final int FIVE_MINUTES = 5 * 60_000;

  @Inject
  @Named("appName")
  private String appName;
  private final String reverseQuery;

  @Inject
  @Any
  private Instance<Cache> candidateCaches;

  private Cache cache;

  public SessionManager(){
    this.reverseQuery = appName + "_reverse";
  }

  @Inject
  public SessionManager(@Named("appName") String appName, Cache cache) {
    this.appName = appName;
    reverseQuery = appName + "_reverse";
    this.cache = cache;
  }

  @PostConstruct
  private void setup() {
    Cache lastResort = null;
    for (Cache candidate : candidateCaches) {
      if (!(candidate instanceof MapCache)) {
        this.cache = candidate;
        return;
      }
      lastResort = candidate;
    }
    cache = lastResort;
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
    return getExistingSession(msisdn) != null;
  }

  public Session getExistingSession(String msisdn) {
    return cache.get(msisdn, DEFAULT_TENANT, reverseQuery, Session.class);
  }
}
