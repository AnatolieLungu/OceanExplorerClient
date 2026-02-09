package com.example.application.client;

import com.example.application.entity.EchoData;
import com.example.application.entity.SectorInfo;
import com.example.application.entity.ShipData;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

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
        .uri("/shipBaseServerAPI/allSectorData")
        .retrieve()
        .bodyToMono(SectorInfo[].class)
        .block();
    return map != null ?Arrays.asList(map) : List.of();
  }
}
