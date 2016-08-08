package com.hextremelabs.ussd.lib.handler;

import com.hextremelabs.ussd.lib.exception.InvocationException;
import com.hextremelabs.ussd.lib.ui.model.ScreenType;
import java.util.Map;

/**
 * A handler for processing callbacks. Applications using this library are required to provide an implementation of this
 * interface as an injectable component (e.g EJB, CDI bean or a discoverable POJO).
 *
 * Every screen of type {@link ScreenType#TEXT_INPUT} collects data and keeps it in the user's session. This is stored
 * as a mapping of {@code screen_name (String) -> input_data (String)}. For example a screen named {@code "prompt"} that
 * received text input {@code "Sayo"} would store it as {@code "prompt" -> "Sayo"}.
 *
 * Likewise every callback produces a * result of type {@link String[]} (which could be null or empty). This is stored
 * as a mapping of {@code screen_name$Result (String) -> execution_result (String[])}. For example a screen named
 * {@code "stateInput"} that has a callback named {@code "getUniversities"} gets the universities in a state. After
 * execution the session would contain mapping {@code "stateInput$Result" -> ["UI", "LAUTECH", "Lead City"]}.
 *
 * A mapping of all data collected and all callback results in a user's session is presented presented to the invoke
 * method. The implementation is free to pick any piece of data from the mapping required to fulfill an operation.
 *
 * @author Sayo Oladeji
 */
public interface UssdHandler {

  String[] invoke(String operation, Map<String, Object> data) throws InvocationException;
}
