package com.hextremelabs.ussd.lib.exception;

/**
 * Indicates that an error occurred while invoking a screen's callback.
 *
 * @author Sayo Oladeji
 */
public class InvocationException extends Exception {

  private static final long serialVersionUID = 1L;

  public InvocationException(String operation, Exception cause) {
    super(operation, cause);
  }
}
