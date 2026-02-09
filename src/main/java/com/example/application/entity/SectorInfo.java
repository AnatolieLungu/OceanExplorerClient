package com.example.application.entity;

import lombok.Data;

@Data
public class SectorInfo {
  private String shipId;
  private  Ground ground;
  private int depth;
  private int sectorX;
  private int sectorY;
}
