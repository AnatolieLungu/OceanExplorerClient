package com.example.application.components;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.dom.Style;

public class AppTitle {

  private static final String TEXT = "OCEAN EXPLORER";

  public static H3 create() {
    H3 title = new H3(TEXT);

    title.getStyle()
        .setColor("#6699ff")
        .setMargin("25px 0 15px 0")
        .setFontSize("26px")
        .setFontWeight("500")
        .setTextAlign(Style.TextAlign.CENTER)
        .setOpacity("0.9")
        .set("letter-spacing", "0.5px");

    return title;
  }
}
