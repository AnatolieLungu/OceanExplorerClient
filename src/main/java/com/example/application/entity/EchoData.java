package com.example.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EchoData {
  private List<Echo> echos;
  private List<NotNavigable> notNavigable;
}
