package com.example.application.service;

import com.example.application.client.ShipBaseServerClient;
import com.example.application.client.ShipClient;
import com.example.application.entity.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShipCommandService {

  private final ShipClient shipClient;
  private final ShipBaseServerClient shipBaseServerClient;

  private final List<Vec2D> unavailableDirections = new ArrayList<>();

  public ShipCommandService(ShipClient shipClient, ShipBaseServerClient shipBaseServerClient) {
    this.shipClient = shipClient;
    this.shipBaseServerClient = shipBaseServerClient;
  }

  public String launch(String name, int x, int y, int dx, int dy) {
    String response = shipClient.launch(name, x, y, dx, dy);
    if (response != null && !response.equals("Error")) {
      return response;
    }
    return response;
  }

  public List<SectorInfo> loadMap() {
    return shipBaseServerClient.loadMap();
  }

  public List<ShipData> getShips() {
    return shipBaseServerClient.loadShips();
  }

  public void exit(String shipId){
    shipClient.exit(shipId);
  }

  public List<Vec2D> getUnavailableDirections(ShipData shipData) {
    unavailableDirections.clear();

    EchoData echoData = shipClient.radar(shipData.getShipId());

    List<Echo> echos = echoData.getEchos();

    List<NotNavigable> request = echoData.getNotNavigable();

    for (NotNavigable notNavigable : request) {
      unavailableDirections.add(new Vec2D(notNavigable.getVec2()[0], notNavigable.getVec2()[1]));
    }
    int dx = shipData.getSectorX();
    int dy = shipData.getSectorY();
    setUnavailableDirectionsForCoordinates(shipData.getDirectionX(), shipData.getDirectionY());
    for (Echo echo : echos) {
      int echoX = dx + echo.getSector().getVec2()[0];
      int echoY = dy + echo.getSector().getVec2()[1];
      isOutsideValidArea(echo, echoX, echoY);
    }
    return unavailableDirections;
  }

  private void isOutsideValidArea(Echo echo, int echoX, int echoY) {
    if (echoX > 99 || echoY > 99 || echoY < 0 || echoX < 0) {
      addIfNotPresent(
          echo.getSector().getVec2()[0],
          echo.getSector().getVec2()[1]
      );
    }
  }

  private void setUnavailableDirectionsForCoordinates(int dx, int dy) {
    //N and S
    if (dx == 0) {
      addIfNotPresent(-1, 0);
      addIfNotPresent(1, 0);
    }// NE and SW
    else if (dx == dy) {
      addIfNotPresent(-1, 1);
      addIfNotPresent(1, -1);
    }
    // E and W
    else if (dy == 0) {
      addIfNotPresent(0, 1);
      addIfNotPresent(0, -1);
    }
    // SE and NW
    else if (dx == -dy) {
      addIfNotPresent(1, 1);
      addIfNotPresent(-1, -1);
    }
  }

  private void addIfNotPresent(int x, int y) {
    Vec2D vec = new Vec2D(x, y);
    if (!unavailableDirections.contains(vec)) {
      unavailableDirections.add(vec);
    }
  }

  public Vec2D navigate(String shipId,Directions actualDirection,Directions expectedDirection) {

    // Gleiche Richtung
    if (actualDirection == expectedDirection) {
      return shipClient.navigation(shipId,Course.Forward,Rudder.Center);
    }
    int diff = ( actualDirection.ordinal() - expectedDirection.ordinal() + 8) % 8;
    // Gleiche Richtung Rückwerz
    if (diff == 4) {
      return shipClient.navigation(shipId,Course.Backward,Rudder.Center);
    }

    // 45° → vorwärts + Rudder
    if (diff == 1 || diff == 7) {
      Course course = Course.Forward;
      Rudder rudder = (diff == 1) ? Rudder.Left : Rudder.Right;
      return shipClient.navigation(shipId, course, rudder);
    }

    // 135° → rückwärts + Rudder-Korrektur
    if (diff == 3 || diff == 5) {
      Course course = Course.Backward;
      Rudder rudder = (diff == 3) ? Rudder.Left : Rudder.Right;
      return shipClient.navigation(shipId, course, rudder);
    }
    return null;
  }

}
