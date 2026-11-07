package com.hotelbooking.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.user.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserApiTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository users;

  @Test
  void registerIssuesOneHourUserToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"ada\",\"password\":\"password1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInSeconds").value(3600))
            .andReturn();

    String token = token(result);
    SignedJWT jwt = SignedJWT.parse(token);
    long lifetimeSeconds =
        (jwt.getJWTClaimsSet().getExpirationTime().getTime()
                - jwt.getJWTClaimsSet().getIssueTime().getTime())
            / 1000;
    org.junit.jupiter.api.Assertions.assertEquals(3600, lifetimeSeconds);
    org.junit.jupiter.api.Assertions.assertEquals(
        "USER", jwt.getJWTClaimsSet().getStringClaim("role"));
    org.junit.jupiter.api.Assertions.assertEquals("ada", jwt.getJWTClaimsSet().getSubject());

    mockMvc
        .perform(
            get("/missing").header("Authorization", "Bearer " + token).header("traceId", "t-1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.traceId").value("t-1"));
  }

  @Test
  void authRejectsWrongPassword() throws Exception {
    mockMvc
        .perform(
            post("/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void userCannotCreateAccounts() throws Exception {
    String token = register("grace", "password1");

    mockMvc
        .perform(
            post("/user")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"other\",\"password\":\"password1\",\"role\":\"USER\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void missingTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(post("/user").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Authentication required"));
  }

  @Test
  void adminCreatesUpdatesAndDeletesUser() throws Exception {
    String admin = adminToken();

    MvcResult created =
        mockMvc
            .perform(
                post("/user")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"bob\",\"password\":\"password1\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("bob"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();
    String id =
        com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")
            .toString();

    mockMvc
        .perform(
            patch("/user")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + id + ",\"username\":\"bobby\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("bobby"))
        .andExpect(jsonPath("$.role").value("ADMIN"));

    mockMvc
        .perform(delete("/user").param("id", id).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(delete("/user").param("id", id).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCannotDeleteUserWithBookings() throws Exception {
    String admin = adminToken();
    long id = users.findByUsername("demo").orElseThrow().getId();

    mockMvc
        .perform(
            delete("/user")
                .param("id", Long.toString(id))
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("User has bookings"));

    mockMvc
        .perform(
            post("/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"password1\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void generatedTraceIdIsCopiedIntoNotFound() throws Exception {
    String token = register("trace-" + System.nanoTime(), "password1");
    MvcResult result =
        mockMvc
            .perform(get("/missing").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andReturn();
    String traceId = result.getResponse().getHeader("traceId");
    org.junit.jupiter.api.Assertions.assertNotNull(traceId);
    org.junit.jupiter.api.Assertions.assertEquals(
        traceId,
        com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.traceId"));
  }

  @Test
  void duplicateUsernameConflicts() throws Exception {
    mockMvc
        .perform(
            post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"password1\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Username already exists"));
  }

  @Test
  void shortPasswordIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"shorty\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  private String adminToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/user/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"admin12345\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return token(result);
  }

  private String register(String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return token(result);
  }

  private static String token(MvcResult result) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
  }
}
