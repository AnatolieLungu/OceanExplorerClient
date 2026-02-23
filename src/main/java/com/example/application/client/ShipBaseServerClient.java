package com.example.application.client;
import com.example.application.entity.SectorInfo;
import com.example.application.entity.ShipData;
import com.example.application.entity.ShipSector;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ShipBaseServerClient {

  private final WebClient webClient;

  public ShipBaseServerClient(WebClient.Builder builder) {
    this.webClient = builder
        .baseUrl("http://localhost:8090")
        .build();
  }

  public List<ShipData> loadShips() {
    ShipData[] shipData = webClient.get()
        .uri("/shipBaseServerAPI/getAllShipData")
        .retrieve()
        .bodyToMono(ShipData[].class)
        .block();
    return shipData != null ? Arrays.asList(shipData) :  List.of();
  }


  public List<SectorInfo> loadMap(){
    SectorInfo[] map = webClient.get()
        .uri("/shipBaseServerAPI/allSectorInfo")
        .retrieve()
        .bodyToMono(SectorInfo[].class)
        .block();
    return map != null ?Arrays.asList(map) : List.of();
  }

  public Map<String,List<ShipSector>> loadRoutes(){
    return webClient.get()
        .uri("/shipBaseServerAPI/getShipRoute")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, List<ShipSector>>>() {})
        .block(Duration.ofSeconds(10));
  }
}
