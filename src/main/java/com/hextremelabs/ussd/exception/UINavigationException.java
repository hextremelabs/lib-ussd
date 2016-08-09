package com.hextremelabs.ussd.exception;

/**
 * Indicates that there was an error in screen navigation (if the next screen could not be found).
 *
 * @author Sayo Oladeji
 */
public class UINavigationException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new instance of <code>UINavigationException</code> without detail message.
   */
  public UINavigationException() {
  }

  /**
   * Constructs an instance of <code>UINavigationException</code> with the specified detail message.
   *
   * @param msg the detail message.
   */
  public UINavigationException(String msg) {
    super(msg);
  }
}
