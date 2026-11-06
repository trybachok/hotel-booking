package com.hotelbooking.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookingErrorDtoTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void unknownPathReturnsErrorDto() throws Exception {
    mockMvc
        .perform(get("/missing").header("traceId", "trace-booking"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.path").value("/missing"))
        .andExpect(jsonPath("$.traceId").value("trace-booking"))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
