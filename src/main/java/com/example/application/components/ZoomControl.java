package com.example.application.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.function.Consumer;

public class ZoomControl extends VerticalLayout {

  private static final double[] ZOOM_LEVELS = {1, 2, 3, 5, 8};

  private int currentIndex = 0;
  private final Span label = new Span("1x");
  private Consumer<Double> zoomChangeListener;

  public ZoomControl() {
    setPadding(true);
    setSpacing(false);
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    setWidth("48px");

    getStyle()
        .setBackground("rgba(0, 0, 0, 0.35)")
        .set("backdrop-filter", "blur(4px)")
        .set("-webkit-backdrop-filter", "blur(4px)")
        .setBorderRadius("10px")
        .set("gap", "4px")
        .setPadding("6px");

    Button zoomIn = createZoomButton("+");
    zoomIn.addClickListener(e -> {
      if (currentIndex < ZOOM_LEVELS.length - 1) {
        currentIndex++;
        updateLabel();
        fireZoomChange();
      }
    });

    Button zoomOut = createZoomButton("\u2212"); // minus sign
    zoomOut.addClickListener(e -> {
      if (currentIndex > 0) {
        currentIndex--;
        updateLabel();
        fireZoomChange();
      }
    });

    label.getStyle()
        .setColor("white")
        .setFontSize("13px")
        .set("font-weight", "600")
        .set("user-select", "none")
        .set("text-align", "center")
        .setWidth("100%");

    add(zoomIn, label, zoomOut);
  }

  private Button createZoomButton(String text) {
    Button btn = new Button(text);
    btn.getStyle()
        .setWidth("36px")
        .setHeight("36px")
        .set("min-width", "36px")
        .setPadding("0")
        .setBackground("rgba(255, 255, 255, 0.15)")
        .setColor("white")
        .setBorder("1px solid rgba(255, 255, 255, 0.25)")
        .setBorderRadius("8px")
        .setCursor("pointer")
        .setFontSize("18px")
        .set("font-weight", "bold")
        .set("line-height", "1")
        .set("transition", "background 0.15s");
    return btn;
  }

  private void updateLabel() {
    double zoom = ZOOM_LEVELS[currentIndex];
    label.setText((int) zoom + "x");
  }

  private void fireZoomChange() {
    if (zoomChangeListener != null) {
      zoomChangeListener.accept(ZOOM_LEVELS[currentIndex]);
    }
  }

  public void setZoomChangeListener(Consumer<Double> listener) {
    this.zoomChangeListener = listener;
  }

  /**
   * Called externally (e.g. from mouse wheel zoom) to sync the label.
   */
  public void setZoomLevel(double zoom) {
    for (int i = 0; i < ZOOM_LEVELS.length; i++) {
      if (ZOOM_LEVELS[i] == zoom) {
        currentIndex = i;
        updateLabel();
        return;
      }
    }
  }
}
