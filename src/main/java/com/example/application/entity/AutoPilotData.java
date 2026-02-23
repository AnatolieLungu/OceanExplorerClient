package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AutoPilotData {
  private String shipId;
  private ShipPosition shipPosition;
  private List<SectorData> sectorDataList;
}
