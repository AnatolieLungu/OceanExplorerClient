package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectorData {
  private String shipId;
  private Ground ground;
  private int sectorX;
  private int sectorY;
  private int height;
  private int depth;
  private float stddev;
}
