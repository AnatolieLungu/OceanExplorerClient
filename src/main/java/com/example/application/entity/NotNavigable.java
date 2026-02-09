package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotNavigable {
  private int[] vec2;
  public NotNavigable(Vec2D vec) {
    vec2 = new int[]{vec.getX(), vec.getY()};
  }
}
