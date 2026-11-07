package com.hotelbooking.booking.hotelclient;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HotelClientConfig {

  @Bean
  @LoadBalanced
  RestClient.Builder hotelLoadBalancedBuilder(
      @Value("${hotel.client.connect-timeout:2s}") Duration connectTimeout,
      @Value("${hotel.client.read-timeout:2s}") Duration readTimeout) {
    return plainBuilder(connectTimeout, readTimeout);
  }

  @Bean(name = "hotelRestClient")
  @ConditionalOnProperty(name = "hotel.client.base-url")
  RestClient directHotelRestClient(
      @Value("${hotel.client.base-url}") String baseUrl,
      @Value("${hotel.client.connect-timeout:2s}") Duration connectTimeout,
      @Value("${hotel.client.read-timeout:2s}") Duration readTimeout) {
    return plainBuilder(connectTimeout, readTimeout).baseUrl(baseUrl).build();
  }

  @Bean(name = "hotelRestClient")
  @ConditionalOnMissingBean(name = "hotelRestClient")
  RestClient discoveryHotelRestClient(@LoadBalanced RestClient.Builder hotelLoadBalancedBuilder) {
    return hotelLoadBalancedBuilder.baseUrl("http://hotel-service").build();
  }

  private static RestClient.Builder plainBuilder(Duration connectTimeout, Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(readTimeout);
    return RestClient.builder().requestFactory(factory);
  }
}
