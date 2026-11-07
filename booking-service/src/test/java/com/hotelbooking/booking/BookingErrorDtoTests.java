package com.hotelbooking.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BookingErrorDtoTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void unknownPathWithoutTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/missing").header("traceId", "trace-booking"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.traceId").value("trace-booking"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void generatedTraceIdIsCopiedIntoTheErrorBody() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/missing")).andExpect(status().isUnauthorized()).andReturn();
    String traceId = result.getResponse().getHeader("traceId");
    assertNotNull(traceId);
    assertFalse(traceId.isBlank());
    assertEquals(traceId, JsonPath.read(result.getResponse().getContentAsString(), "$.traceId"));
  }
}
