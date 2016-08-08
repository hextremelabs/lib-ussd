package com.hextremelabs.ussd.lib.exception;

import com.hextremelabs.ussd.lib.internal.Internal.Joiner;
import com.hextremelabs.ussd.lib.ui.model.Validation;
import static com.hextremelabs.ussd.lib.ui.model.Validation.REGEX;

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
    String expectation = this.validation == REGEX ? null : this.validation.name().toLowerCase() + " expected.";
    message = Joiner.on(" ").skipNull().join("Invalid entry", command, ".", expectation, "Please try again.");
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
