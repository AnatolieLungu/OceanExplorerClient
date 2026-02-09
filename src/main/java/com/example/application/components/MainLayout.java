package com.example.application.components;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class MainLayout extends HorizontalLayout {

  public MainLayout() {
    setWidthFull();
    setHeight("960");
    setAlignItems(Alignment.START);
    setSpacing(true);
    setPadding(true);
    setJustifyContentMode(JustifyContentMode.CENTER);
  }
}
