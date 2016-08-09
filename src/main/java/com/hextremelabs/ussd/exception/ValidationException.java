package com.hextremelabs.ussd.exception;

import com.hextremelabs.ussd.internal.Internal;
import com.hextremelabs.ussd.ui.model.Validation;

/**
 * Indicates that an input failed its associated validation constraint.
 *
 * @author Sayo Oladeji
 */
public class ValidationException extends Exception {

  private static final long serialVersionUID = 1L;

  private final Validation validation;
  private final String command;
  private final String message;

  public ValidationException(Validation validation, String command) {
    super();
    this.validation = validation;
    this.command = command;
    String expectation = this.validation == Validation.REGEX ? null : this.validation.name() + " input expected.";
    message = Internal.Joiner.on(" ").skipNull().join("Invalid entry", command + ".", expectation, "Please try again.");
  }

  public Validation getValidation() {
    return validation;
  }

  public String getCommand() {
    return command;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
