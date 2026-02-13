package com.example.application.entity;

import lombok.Getter;

@Getter
public enum Directions {

  NORTH("N", 0, 1),
  NORTH_EAST("NE", 1, 1),
  EAST("E", 1, 0),
  SOUTH_EAST("SE", 1, -1),
  SOUTH("S", 0, -1),
  SOUTH_WEST("SW", -1, -1),
  WEST("W", -1, 0),
  NORTH_WEST("NW", -1, 1);

  private final String shortName;
  private final int dx;
  private final int dy;

  Directions(String shortName, int dx, int dy) {
    this.shortName = shortName;
    this.dx = dx;
    this.dy = dy;
  }

  public static Directions fromShortName(String shortName) {
    if (shortName == null)
      return null;
    for (Directions dir : values()) {
      if (dir.shortName.equalsIgnoreCase(shortName)) {
        return dir;
      }
    }
    return null;
  }
  public static String nameFromDirection(Directions directions){
    if(directions == null)
      return null;
    return directions.getShortName();
  }

  public static Directions fromDelta(int dx, int dy) {
    for (Directions dir : values()) {
      if (dir.dx == dx && dir.dy == dy) {
        return dir;
      }
    }
    return null;
  }
}
