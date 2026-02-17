package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipSector {
  private Long id;
  private String shipId;
  private int shipSectorX;
  private int shipSectorY;
}
