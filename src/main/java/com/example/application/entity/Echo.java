package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Echo {
  private Sector sector;
  private int height;
  private Ground ground;
}
