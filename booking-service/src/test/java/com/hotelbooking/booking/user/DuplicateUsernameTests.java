package com.hotelbooking.booking.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DuplicateUsernameTests {

  @LocalServerPort private int port;

  @Test
  void parallelRegisterConflictsInsteadOfFailing() throws Exception {
    String username = "dup-" + System.nanoTime();
    String body = "{\"username\":\"" + username + "\",\"password\":\"password1\"}";
    HttpClient http = HttpClient.newHttpClient();
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> left = pool.submit(() -> post(http, body, go));
      Future<Integer> right = pool.submit(() -> post(http, body, go));
      go.countDown();
      List<Integer> statuses = List.of(left.get(), right.get());
      assertTrue(statuses.contains(201), statuses.toString());
      assertTrue(statuses.contains(409), statuses.toString());
      assertEquals(2, statuses.size());
    } finally {
      pool.shutdownNow();
    }
  }

  private int post(HttpClient http, String body, CountDownLatch go) throws Exception {
    go.await();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/user/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
  }
}
