package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipData {
  private String shipId;
  private String shipName;
  private int sectorX;
  private int sectorY;
  private int directionX;
  private int directionY;
}
