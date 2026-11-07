package com.hotelbooking.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

final class TraceIds {

  private TraceIds() {}

  static String current(WebRequest request) {
    String header = header(request.getHeader(TraceIdMdcFilter.TRACE_ID));
    if (header != null) {
      return header;
    }
    if (request instanceof ServletWebRequest servletRequest) {
      return current(servletRequest.getRequest());
    }
    return fromMdc();
  }

  static String current(HttpServletRequest request) {
    String header = header(request.getHeader(TraceIdMdcFilter.TRACE_ID));
    if (header != null) {
      return header;
    }
    Object attribute = request.getAttribute(TraceIdMdcFilter.TRACE_ID);
    if (attribute instanceof String value) {
      String stored = header(value);
      if (stored != null) {
        return stored;
      }
    }
    return fromMdc();
  }

  private static String fromMdc() {
    return header(MDC.get(TraceIdMdcFilter.TRACE_ID));
  }

  private static String header(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
