package com.example.application.client;

import com.example.application.entity.*;
import com.example.application.entity.ScanResult;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ShipClient {

  private final WebClient webClient;

  public ShipClient(WebClient.Builder builder) {
    this.webClient = builder
        .baseUrl("http://localhost:8080")
        .build();
  }

  public String launch(String name, int x, int y, int dx, int dy) {
    try {
      return webClient.post()
          .uri(uriBuilder -> uriBuilder
              .path("/api/ship/launch")
              .queryParam("name", name)
              .queryParam("x", x)
              .queryParam("y", y)
              .queryParam("dx", dx)
              .queryParam("dy", dy)
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      return e.getResponseBodyAsString();
    }
  }

  public EchoData radar(String shipId) {
    return webClient.get()
        .uri("/api/ship/radar?shipId={shipId}", shipId)
        .retrieve()
        .bodyToMono(EchoData.class)
        .block();
  }

  public @Nullable Vec2D navigation(String shipId, Course course, Rudder rudder) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/ship/navigate")
            .queryParam("shipId", shipId)
            .queryParam("course", course)
            .queryParam("rudder", rudder)
            .build())
        .retrieve()
        .bodyToMono(Vec2D.class)
        .block();
  }

  public ScanResult scan(String shipId) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/ship/scan")
            .queryParam("shipId",shipId)
            .build())
        .retrieve()
        .bodyToMono(ScanResult.class)
        .block();
  }

  public AutoPilotData autoPilot(String shipId) {
    return webClient.post()
        .uri(uriBuilder -> uriBuilder
            .path("/api/ship/autoPilot")
            .queryParam("shipId", shipId)
            .build())
        .retrieve()
        .bodyToMono(AutoPilotData.class)
        .block();
  }

  public void exit(String shipId) {
    webClient.post()
        .uri(uriBuilder -> uriBuilder
            .path("/api/ship/exit")
            .queryParam("shipId",shipId)
            .build())
        .retrieve()
        .bodyToMono(Void.class)
        .block();
  }

}
