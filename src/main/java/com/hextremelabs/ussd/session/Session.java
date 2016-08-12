package com.hextremelabs.ussd.session;

import com.hextremelabs.ussd.dto.UssdRequest;
import com.hextremelabs.ussd.internal.Internal.Pair;
import com.hextremelabs.ussd.ui.model.Screen;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static com.hextremelabs.ussd.internal.Internal.currentDate;

/**
 * Represents a user's session.
 *
 * @author Sayo Oladeji
 */
public class Session implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String id;
  private final String msisdn;
  private final String provider;

  private final Deque<Pair<Screen, String>> navigation;
  private final Map<String, Object> data;

  private final Date timestamp;

  public Session(UssdRequest request) {
    this.id = request.getSessionId();
    this.msisdn = request.getMsisdn();
    this.provider = request.getProvider();
    this.navigation = new ArrayDeque<>();
    this.data = new HashMap<>();
    this.timestamp = currentDate();
  }

  public String getId() {
    return id;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public String getProvider() {
    return provider;
  }

  public Deque<Pair<Screen, String>> getNavigation() {
    return navigation;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void pushScreen(Screen screen) {
    navigation.push(Pair.newMutablePairOf(screen, null));
  }

  public void pushScreen(Screen screen, String renderedScreen) {
    navigation.push(Pair.newImmutablePairOf(screen, renderedScreen));
  }

  public Pair<Screen, String> peekLastScreen() {
    return navigation.peek();
  }

  public Pair<Screen, String> pollLastScreen() {
    return navigation.poll();
  }

  public void putData(String key, Object value) {
    data.put(key, value);
  }

  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "Session{" + "id=" + id + ", msisdn=" + msisdn + ", provider=" + provider + ", navigation=" + navigation
        + ", data=" + data + ", timestamp=" + timestamp + '}';
  }
}
