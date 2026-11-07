package com.hotelbooking.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(OutputCaptureExtension.class)
class RestExceptionHandlerTests {

  @Test
  void unexpectedErrorLogsTheException(CapturedOutput output) {
    RestExceptionHandler handler = new RestExceptionHandler();
    IllegalStateException failure = new IllegalStateException("boom");
    handler.handleUnexpected(
        failure, new ServletWebRequest(new MockHttpServletRequest("GET", "/boom")));
    assertThat(output).contains("IllegalStateException");
    assertThat(output).contains("boom");
  }
}
