package com.hotelbooking.booking.hotelclient;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HotelClientConfig {

  @Bean
  @LoadBalanced
  RestClient.Builder hotelRestClientBuilder(
      @Value("${hotel.client.connect-timeout:2s}") Duration connectTimeout,
      @Value("${hotel.client.read-timeout:2s}") Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(readTimeout);
    return RestClient.builder().requestFactory(factory);
  }

  @Bean
  RestClient hotelRestClient(@LoadBalanced RestClient.Builder hotelRestClientBuilder) {
    return hotelRestClientBuilder.build();
  }
}
