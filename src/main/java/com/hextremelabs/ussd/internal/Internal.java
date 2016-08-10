package com.hextremelabs.ussd.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.NoResultException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This is a very shameful attempt at eliminating a dependency. Look away there is nothing to see here.
 *
 * @author Sayo Oladeji
 */
public class Internal {

  private Internal() {
  }

  public static Date currentDate() {
    return new Date();
  }

  private static long diffFromNow(Date date) {
    return absoluteDistance(date, currentDate());
  }

  private static long absoluteDistance(Date start, Date end) {
    return Math.abs(start.getTime() - end.getTime());
  }

  public static abstract class Pair<K, V> implements Serializable {

    protected static final long serialVersionUID = 1L;

    public static <K, V> ImmutablePair<K, V> newImmutablePairOf(final K key, final V value) {
      return ImmutablePair.of(key, value);
    }

    public static <K, V> MutablePair<K, V> newMutablePairOf(final K key, final V value) {
      return MutablePair.of(key, value);
    }

    public abstract K getKey();

    public abstract void setKey(K key);

    public abstract V getValue();

    public abstract void setValue(V value);
  }

  public static class MutablePair<K, V> extends Pair<K, V> implements Serializable {

    private K key;
    private V value;

    public MutablePair() {
    }

    private MutablePair(final K key, final V value) {
      this.key = key;
      this.value = value;
    }

    static <K, V> MutablePair<K, V> of(final K key, final V value) {
      return new MutablePair<K, V>(key, value);
    }

    @Override
    @XmlElement(name = "key")
    public K getKey() {
      return this.key;
    }

    @Override
    public void setKey(K key) {
      this.key = key;
    }

    @Override
    @XmlElement(name = "value")
    public V getValue() {
      return this.value;
    }

    @Override
    public void setValue(V value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "MutablePair{" + "key=" + key + ", value=" + value + '}';
    }
  }

  public static final class ImmutablePair<K, V> extends Pair<K, V> {

    private final K KEY;
    private final V VALUE;

    private ImmutablePair(final K key, final V value) {
      this.KEY = key;
      this.VALUE = value;
    }

    public static <K, V> ImmutablePair<K, V> of(final K key, final V value) {
      return new ImmutablePair<K, V>(key, value);
    }

    @Override
    public K getKey() {
      return KEY;
    }

    @Override
    public void setKey(K key) {
      throw new UnsupportedOperationException("Cannot modify the key of an immutable pair!");
    }

    @Override
    public V getValue() {
      return VALUE;
    }

    @Override
    public void setValue(V value) {
      throw new UnsupportedOperationException("Cannot modify the value of an immutable pair!");
    }

    @Override
    public String toString() {
      return "ImmutablePair{" + "KEY=" + KEY + ", VALUE=" + VALUE + '}';
    }
  }

  @InterceptorBinding
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD})
  public static @interface Log {

    @Nonbinding
    public boolean secure() default false;
  }

  @Log
  @Interceptor
  @Priority(Interceptor.Priority.APPLICATION + 5)
  @Dependent
  public static class TraceLogger implements Serializable {

    private static final long serialVersionUID = 1L;

    // TODO(oluwasayo): Log this in a more sophisticated datastore like Reddis.
    private final Logger L = LoggerFactory.getLogger(TraceLogger.class);

    private static String join(Map<String, Object> argMap) {
      if (argMap == null) {
        return "";
      }

      StringBuilder sb = new StringBuilder();
      argMap.keySet().stream().forEach((entry) -> {
        sb.append(entry).append(" = ").append(argMap.get(entry)).append(", ");
      });
      if (sb.length() > 1) {
        sb.delete(sb.length() - 2, sb.length());
      }
      return sb.toString();
    }

    /**
     * Use at your own risk. This may kill your kitten or blow up your Playstation.
     *
     * @param args
     * @return
     */
    public static Map<String, Object> build(Object... args) {
      Map<String, Object> result = new HashMap<>();
      if (args.length == 0) {
        return result;
      }

      if ((args.length & 1) != 0) { // Odd number of elements in args.
        throw new RuntimeException("Malformed log data! Argument length should be even!");
      }

      for (int i = 0; i < args.length; i += 2) {
        result.put(args[i].toString(), args[i + 1]);
      }

      return result;
    }

    @AroundInvoke
    public Object logAroundInvoke(InvocationContext ic) throws Exception {
      Class clazz = ic.getMethod().getDeclaringClass();
      Log log = ic.getMethod().getAnnotation(Log.class);
      if (DataHelper.hasBlank(log)) {
        log = (Log) clazz.getAnnotation(Log.class);
      }

      boolean secure = (!DataHelper.hasBlank(log) && log.secure());
      Object[] params = new Object[]{clazz.getName(), ic.getMethod().getName(),
        Joiner.on(", ").useForNull("").join(ic.getParameters())};
      L.debug("Entering - {}#{} : " + (secure ? "*****" : "{}"), ic.getMethod().getDeclaringClass().getSimpleName(),
          ic.getMethod().getName(), params);

      Object result = ic.proceed();
      params[2] = result;
      L.debug("Exiting - {}#{} : " + (secure ? "*****" : "{}"), ic.getMethod().getDeclaringClass().getSimpleName(),
          ic.getMethod().getName(), result);

      return result;
    }
  }

  public static class DataHelper {

    private static final Logger L = LoggerFactory.getLogger(DataHelper.class);

    private static final Map<Class, JAXBContext> JAXB_CONTEXT_MAP = new HashMap<>();

    private DataHelper() {
    }

    /**
     * Inspects input for possible null references, or emptiness (if an element is a {@link String}). Because we are
     * dealing primarily with SOAP, a {@link String} "{@code ?}" is also considered empty. Empty {@link Collection}s and
     * {@link Map}s are also considered specially.
     */
    public static boolean hasBlank(Object... inputs) {
      for (Object obj : inputs) {
        if (obj == null) {
          return true;
        }
        if (obj instanceof String) {
          if (((String) obj).isEmpty() || "?".equals(obj)) {
            return true;
          }
        }
        if (obj instanceof Collection) {
          if (((Collection) obj).isEmpty()) {
            return true;
          }
        }
        if (obj instanceof Map) {
          if (((Map) obj).isEmpty()) {
            return true;
          }
        }
        // Arrays are difficult to deal with so I'm not performing a special check on them.
      }

      return false;
    }

    public static <T> String marshalXml(T object) {
      return marshalXml(object, (XmlOptions) null);
    }

    public static <T> String marshalXml(T object, XmlOptions options) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      marshalXml(object, outputStream, options);
      try {
        return outputStream.toString("UTF-8");
      } catch (UnsupportedEncodingException ex) {
        L.error("Error converting byte stream to String. UTF-8 encoding not supported.", ex);
        return null;
      }
    }

    public static <T> void marshalXml(T object, OutputStream outputStream) {
      marshalXml(object, outputStream, null);
    }

    public static <T> void marshalXml(T object, OutputStream outputStream, XmlOptions options) {
      try {
        JAXBContext jaxbContext = JAXB_CONTEXT_MAP.get(object.getClass());
        if (jaxbContext == null) {
          jaxbContext = JAXBContext.newInstance(object.getClass());
          JAXB_CONTEXT_MAP.put(object.getClass(), jaxbContext);
        }

        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        if (options != null) {
          if (options.formattedOutput) {
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, options.formattedOutput);
          }
          if (options.fragment) {
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, options.fragment);
          }
          if (options.noNamespaceSchemaLocation != null) {
            jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, options.noNamespaceSchemaLocation);
          }
          if (options.schemaLocation != null) {
            jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, options.schemaLocation);
          }
          jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, options.encoding);
        }
        jaxbMarshaller.marshal(object, outputStream);
      } catch (JAXBException exc) {
        L.error("An error occured while marshalling an object of type: " + object.getClass().getSimpleName(), exc);
      }
    }

    public static <T> T unmarshalXml(Class<T> type, String xml) {
      if (xml == null) {
        return null;
      }

      T result = null;
      try {
        return unmarshalXml(type, new ByteArrayInputStream(xml.getBytes("UTF-8")));
      } catch (Exception exc) {
        L.warn("Unable to unmarshal the xml. Xml = {}", xml, exc);
      }

      return result;
    }

    public static <T> T unmarshalXml(Class<T> type, InputStream input) {
      if (input == null) {
        return null;
      }

      T result = null;
      try {
        JAXBContext jaxbContext = JAXB_CONTEXT_MAP.get(type);
        if (jaxbContext == null) {
          jaxbContext = JAXBContext.newInstance(type);
          JAXB_CONTEXT_MAP.put(type, jaxbContext);
        }

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        result = (T) unmarshaller.unmarshal(input);
      } catch (Exception exc) {
        L.warn("Unable to unmarshal the xml stream.", exc);
      }

      return result;
    }

    public static final class XmlOptions {

      private boolean fragment = false;
      private boolean formattedOutput = false;
      private String encoding = "UTF-8";
      private String schemaLocation = null;
      private String noNamespaceSchemaLocation = null;

      public XmlOptions fragment(boolean fragment) {
        this.fragment = fragment;
        return this;
      }

      public XmlOptions formattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
        return this;
      }

      public XmlOptions encoding(String encoding) {
        this.encoding = encoding;
        return this;
      }

      public XmlOptions schemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        return this;
      }

      public XmlOptions noNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
        return this;
      }
    }
  }

  public static class Joiner {

    private final String separator;
    private String nullToken;
    private boolean skipNull;

    private Joiner(String separator) {
      this.separator = separator;
    }

    public static Joiner on(String separator) {
      return new Joiner(separator);
    }

    public static Joiner blankJoiner() {
      return on("");
    }

    public Joiner useForNull(String nullToken) {
      this.nullToken = nullToken;
      return this;
    }

    public Joiner skipNull() {
      this.skipNull = true;
      return this;
    }

    public String join(final Collection<Object> collection) {
      return join(collection.toArray());
    }

    public String join(final Object... collection) {
      if (collection == null || collection.length == 0) {
        return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int a = 0; a < collection.length - 1; a++) {
        Object token = collection[a];
        if (token == null) {
          if (skipNull) {
            continue;
          }
          token = nullToken;
        }
        sb.append(token).append(separator);
      }

      if (skipNull && collection[collection.length - 1] == null) {
        sb.delete(sb.length() - separator.length(), sb.length());
      } else {
        sb.append(collection[collection.length - 1]);
      }

      return sb.toString();
    }
  }

  public static interface Cache {

    public Object get(String key, String tenant, String namespace) throws CacheException;

    public Object get(String key, String tenant) throws CacheException;

    public Object get(String key) throws NoResultException, CacheException;

    public <T> T get(String key, String tenant, String namespace, Class<T> type) throws CacheException;

    public <T> T get(String key, String tenant, Class<T> type) throws CacheException;

    public <T> T get(String key, Class<T> type) throws CacheException;

    public void put(String key, Object value) throws CacheException;

    public void put(String key, Object value, Date expiry) throws CacheException;

    public void put(String key, Object value, String tenant) throws CacheException;

    public void put(String key, Object value, String tenant, Date expiry) throws CacheException;

    public void put(String key, Object value, long lifespan) throws CacheException;

    public void put(String key, Object value, String tenant, long lifespan) throws CacheException;

    public void put(String key, Object value, String tenant, String namespace) throws CacheException;

    public void put(String key, Object value, String tenant, String namespace, Date expiry) throws CacheException;

    public void put(String key, Object value, String tenant, String namespace, long lifespan) throws CacheException;

    public void remove(String key, String tenant, String namespace);

    public void remove(String key, String tenant);

    public void remove(String key);

    public void clearAll();

    public void clearAllInstancesOfNamespace(String namespace);

    public void clearNamespace(String tenant, String namespace);

    public void clearTenant(String tenant);

    public abstract boolean isPresent(String key, String tenant, String namespace);

    public boolean isPresent(String key, String tenant);

    public boolean isPresent(String key);
  }

  /**
   *
   * @author prodigy4440
   */
  public static abstract class AbstractCache implements Cache {

    public static final String DEFAULT_TENANT = "DEFAULT_TENANT";
    public static final String DEFAULT_NAMESPACE = "DEFAULT_NAMESPACE";
    public static final Date NO_EXPIRE = new Date(0);

    @Override
    public Object get(String key, String tenant) throws CacheException {
      return get(key, tenant, DEFAULT_NAMESPACE);
    }

    @Override
    public Object get(String key) throws CacheException {
      return get(key, DEFAULT_TENANT, DEFAULT_NAMESPACE);
    }

    @Override
    public <T> T get(String key, String tenant, String namespace, Class<T> type) throws CacheException {
      return type.cast(get(key, tenant, namespace));
    }

    @Override
    public <T> T get(String key, String tenant, Class<T> type) throws CacheException {
      return get(key, tenant, DEFAULT_NAMESPACE, type);
    }

    @Override
    public <T> T get(String key, Class<T> type) throws CacheException {
      return get(key, DEFAULT_TENANT, DEFAULT_NAMESPACE, type);
    }

    @Override
    public void put(String key, Object value) throws CacheException {
      put(key, value, DEFAULT_TENANT, DEFAULT_NAMESPACE, NO_EXPIRE);
    }

    @Override
    public void put(String key, Object value, Date expiry) throws CacheException {
      put(key, value, DEFAULT_TENANT, DEFAULT_NAMESPACE, expiry);
    }

    @Override
    public void put(String key, Object value, String tenant) throws CacheException {
      put(key, value, tenant, DEFAULT_NAMESPACE, NO_EXPIRE);
    }

    @Override
    public void put(String key, Object value, String tenant, Date expiry) throws CacheException {
      put(key, value, tenant, DEFAULT_NAMESPACE, expiry);
    }

    @Override
    public void put(String key, Object value, long lifespan) throws CacheException {
      put(key, value, DEFAULT_TENANT, DEFAULT_NAMESPACE, lifespan);
    }

    @Override
    public void put(String key, Object value, String tenant, long lifespan) throws CacheException {
      put(key, value, tenant, DEFAULT_NAMESPACE, lifespan);
    }

    @Override
    public void put(String key, Object value, String tenant, String namespace) throws CacheException {
      put(key, value, tenant, namespace, NO_EXPIRE);
    }

    @Override
    public void put(String key, Object value, String tenant, String namespace, long lifespan) throws CacheException {
      Date expiry = lifespan == 0 ? NO_EXPIRE : new Date(currentDate().getTime() + lifespan);
      put(key, value, tenant, namespace, expiry);
    }

    @Override
    public void remove(String key, String tenant) {
      remove(key, tenant, DEFAULT_NAMESPACE);
    }

    @Override
    public void remove(String key) {
      remove(key, DEFAULT_TENANT, DEFAULT_NAMESPACE);
    }

    @Override
    public boolean isPresent(String key, String tenant) {
      return isPresent(key, tenant, AbstractCache.DEFAULT_NAMESPACE);
    }

    @Override
    public boolean isPresent(String key) {
      return isPresent(key, AbstractCache.DEFAULT_TENANT, AbstractCache.DEFAULT_NAMESPACE);
    }
  }

  /**
   *
   * @author prodigy4440
   * @author Sayo Oladeji
   */
  @ApplicationScoped
  public static class MapCache extends AbstractCache implements Serializable {

    private static final long serialVersionUID = 1l;
    private static final Logger L = LoggerFactory.getLogger(MapCache.class);

    private static final int CACHE_CLEANUP_INTERVAL = 5;

    private final Map<String, Map<String, Map<String, Object>>> CACHE; // Root -> Tenant -> Namespace -> Key -> Value.
    private final Set<ManagedEntry> MANAGED_ENTRY_SET;

    public MapCache() {
      L.info("Instantiating new MapCache...");
      CACHE = new ConcurrentHashMap<>();
      MANAGED_ENTRY_SET = new ConcurrentSkipListSet<>();
      ConcurrencyManager.scheduleAtFixedRate(new Cleaner(), CACHE_CLEANUP_INTERVAL, CACHE_CLEANUP_INTERVAL, MINUTES);
      L.info("MapCache created...");
    }

    @Override
    public void put(String key, Object value, String tenant, String namespace, Date expiry)
        throws CacheException {
      L.debug("Attempting to insert new data into cache");
      namespace = (DataHelper.hasBlank(namespace)) ? DEFAULT_NAMESPACE : namespace;
      tenant = (DataHelper.hasBlank(tenant)) ? DEFAULT_TENANT : tenant;
      expiry = (DataHelper.hasBlank(expiry)) ? NO_EXPIRE : expiry;

      if (expiry.after(NO_EXPIRE) && diffFromNow(expiry) <= 0) {
        return; // Already expired key. Ignore silently.
      }

      Map<String, Map<String, Object>> tenantMap;
      Map<String, Object> namespaceMap;
      if (CACHE.containsKey(tenant)) {
        tenantMap = CACHE.get(tenant);
        if (tenantMap.containsKey(namespace)) {
          namespaceMap = tenantMap.get(namespace);
          namespaceMap.put(key, value);
        } else {
          namespaceMap = new HashMap<>();
          namespaceMap.put(key, value);
          tenantMap.put(namespace, namespaceMap);
        }
      } else {
        tenantMap = new HashMap<>();
        namespaceMap = new HashMap<>();
        namespaceMap.put(key, value);
        tenantMap.put(namespace, namespaceMap);
        CACHE.put(tenant, tenantMap);
      }

      if (expiry != NO_EXPIRE) {
        ManagedEntry me = new ManagedEntry(key, expiry, tenant, namespace);
        MANAGED_ENTRY_SET.add(me);
      }

      L.debug("New data inserted into cache");
    }

    @Override
    public Object get(String key, String tenant, String namespace) throws CacheException {
      L.debug("Retrieving data from MapCache");
      namespace = (DataHelper.hasBlank(namespace)) ? DEFAULT_NAMESPACE : namespace;
      tenant = (DataHelper.hasBlank(tenant)) ? DEFAULT_TENANT : tenant;

      if (CACHE.containsKey(tenant) && (CACHE.get(tenant).containsKey(namespace))) {
        return CACHE.get(tenant).get(namespace).get(key);
      }

      L.debug(String.format("No entry found for supplied parameters. key=%s; namespace=%s; tenant=%s", key, namespace, tenant));
      return null;
    }

    @Override
    public void remove(String key, String tenant, String namespace) {
      L.debug("Removing entry from MapCache");
      namespace = (DataHelper.hasBlank(namespace)) ? DEFAULT_NAMESPACE : namespace;
      tenant = (DataHelper.hasBlank(tenant)) ? DEFAULT_TENANT : tenant;

      if (CACHE.containsKey(tenant) && (CACHE.get(tenant).containsKey(namespace))) {
        CACHE.get(tenant).get(namespace).remove(key);
      }
    }

    @Override
    public void clearAll() {
      CACHE.clear();
    }

    @Override
    public void clearAllInstancesOfNamespace(String namespace) {
      for (String tenant : CACHE.keySet()) {
        if (CACHE.get(tenant).containsKey(namespace)) {
          CACHE.get(tenant).get(namespace).clear();
        }
      }
    }

    @Override
    public void clearNamespace(String tenant, String namespace) {
      if (CACHE.containsKey(tenant)) {
        if (CACHE.get(tenant).containsKey(namespace)) {
          CACHE.get(tenant).get(namespace).clear();
        }
      }
    }

    @Override
    public void clearTenant(String tenant) {
      if (CACHE.containsKey(tenant)) {
        CACHE.get(tenant).clear();
      }
    }

    @Override
    public boolean isPresent(String key, String tenant, String namespace) {
      if (CACHE.containsKey(tenant) && (CACHE.get(tenant).containsKey(namespace))) {
        return CACHE.get(tenant).get(namespace).containsKey(key);
      }

      return false;
    }

    private class Cleaner implements Runnable {

      @Override
      public void run() {
        L.debug("Starting cache cleanup job");
        // TODO(oluwasayo): Slow O(n) solution. Use binary search to get and eliminate all expierd entries.
        Iterator<ManagedEntry> iterator = MANAGED_ENTRY_SET.iterator();
        long expiredCount = 0;
        while (iterator.hasNext()) {
          ManagedEntry entry = iterator.next();
          if (diffFromNow(entry.getDate()) <= 0) {
            iterator.remove();
            CACHE.get(entry.getTenant()).get(entry.getNamespace()).remove(entry.getKey());
            expiredCount++;
          }
        }
        L.debug("Number of expired items removed: {}", expiredCount);
        L.debug("Cache cleanup finished.");
      }
    }

    private class ManagedEntry implements Comparable<ManagedEntry> {

      private final String key;
      private final String tenant;
      private final String namespace;
      private Date date;

      private ManagedEntry(String key, Date date, String tenant, String namespace) {
        this.key = key;
        this.date = date;
        this.tenant = tenant;
        this.namespace = namespace;
      }

      public String getKey() {
        return key;
      }

      public Date getDate() {
        return date;
      }

      public void setDate(Date date) {
        this.date = date;
      }

      public String getTenant() {
        return tenant;
      }

      public String getNamespace() {
        return namespace;
      }

      @Override
      public int compareTo(ManagedEntry o) {
        return date.compareTo(o.date);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        } else if (this == obj) {
          return true;
        } else if ((obj instanceof ManagedEntry)) {
          return key.equals(((ManagedEntry) obj).key);
        }
        return false;
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(key);
      }
    }
  }

  /**
   *
   * @author prodigy4440
   * @author Sayo Oladeji
   */
  public static class CacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CacheException() {
    }

    public CacheException(String msg) {
      super(msg);
    }
  }

  /**
   *
   * @author prodigy4440
   * @author Sayo Oladeji
   */
  public static class ConcurrencyManager {

    private static final Logger L = LoggerFactory.getLogger(ConcurrencyManager.class);
    private static ScheduledExecutorService EXECUTOR;

    static {
      try {
        EXECUTOR = (ManagedScheduledExecutorService) new InitialContext()
            .lookup("java:comp/DefaultManagedScheduledExecutorService");
      } catch (NamingException ex) {
        // This should never happen as long as we're running within an EE7 compliant container.
        L.warn("Managed executor lookup failed. Falling back to SE executor.", ex);
        EXECUTOR = Executors.newScheduledThreadPool(4);
      }
    }

    private ConcurrencyManager() {
    }

    public static void execute(Runnable task) {
      EXECUTOR.execute(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
      return EXECUTOR.submit(task);
    }

    public static void schedule(Runnable task, long delay, TimeUnit timeUnit) {
      EXECUTOR.schedule(task, delay, timeUnit);
    }

    /**
     * Executes a task at fixed intervals from task start.
     *
     * @param task the task to be executed.
     * @param initialDelay time to delay first execution
     * @param period period between successive executions
     * @param timeUnit the unit of the time specified
     * @return A handle to the task execution that can for example be used to kill it.
     */
    public static Future scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
      return EXECUTOR.scheduleAtFixedRate(task, initialDelay, period, timeUnit);
    }

    /**
     * Executes a task with fixed delay after task termination.
     *
     * @param task the task to be executed.
     * @param initialDelay time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @param timeUnit the unit of the time specified
     * @return A handle to the task execution that can for example be used to kill it.
     */
    public static Future scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit timeUnit) {
      return EXECUTOR.scheduleWithFixedDelay(task, initialDelay, delay, timeUnit);
    }
  }
}
