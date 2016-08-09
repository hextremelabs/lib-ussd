package com.hextremelabs.ussd.exception;

import org.junit.Test;

import static com.hextremelabs.ussd.ui.model.Validation.NUMERIC;
import static com.hextremelabs.ussd.ui.model.Validation.REGEX;
import static org.junit.Assert.assertEquals;

/**
 * @author Sayo Oladeji
 */
public class ValidationExceptionTest {

  @Test
  public void getMessage() throws Exception {
    ValidationException ex = new ValidationException(NUMERIC, "abc");
    assertEquals("Invalid entry abc. NUMERIC input expected. Please try again.", ex.getMessage());

    ex = new ValidationException(REGEX, "abc");
    assertEquals("Invalid entry abc. Please try again.", ex.getMessage());
  }
}