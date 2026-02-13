package com.example.application.components;

import com.example.application.entity.Directions;
import com.example.application.entity.Vec2D;
import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Setter;

import java.util.function.Consumer;

import java.util.*;

@SpringComponent
@UIScope
public class Navigation extends VerticalLayout {

  private final Div circleContainer;
  @Setter
  private Consumer<String> directionListener;
  private String currentDirection = "N";
  private final Image shipImg;

  public Navigation() {
    // Container for the circular layout
    circleContainer = setStyleDivDirections();

    // Image from a control panel
    shipImg = createImageFromDivDirections();

    // Radius of the circle
    addButtonsToDivDirections();

    add(circleContainer);
    setAlignItems(Alignment.CENTER);
  }

  private void addButtonsToDivDirections() {
    String[][] buttons = {
        { "N", "0" },
        { "NE", "45" },
        { "E", "90" },
        { "SE", "135" },
        { "S", "180" },
        { "SW", "225" },
        { "W", "270" },
        { "NW", "315" }
    };

    // Create and position each button
    for (int i = 0; i < buttons.length; i++) {
      String label = buttons[i][0];
      double angle = Double.parseDouble(buttons[i][1]);


      Button button = new Button(label);

      // Calculate position (convert angle to radians)
      // Subtract 90° to start from top (North)
      double angleRad = Math.toRadians(angle - 90);
      double radius = 130;
      double x = radius * Math.cos(angleRad);
      double y = radius * Math.sin(angleRad);

      // Center the button and apply position
      button.getStyle()
          .set("position", "absolute")
          .set("left", (130 + x - 30) + "px")  // 200 = center, 40 = button width/2
          .set("top", (130 + y - 15) + "px")   // 200 = center, 20 = button height/2
          .set("width", "60px")
          .set("height", "30px")
          .set("font-size", "18px");

      button.addClickListener(e -> {
        currentDirection = label;
     //   rotateShipOnControlPanel(shipImg, directionIndex);
        if (directionListener != null) {
          directionListener.accept(label);
        }
        resetAllDirectionsToRed();

      });
      circleContainer.add(button);
    }
  }

  private Image createImageFromDivDirections() {
    final Image shipImg;
    shipImg = new Image("images/ship.png", "Ship");
    shipImg.getStyle()
        .setWidth("120px")
        .setHeight("120px")
        .set("object-fit", "contain")
        .setPosition(Style.Position.ABSOLUTE)
        .set("left", "50%")
        .set("top", "50%")
        .set("transform", "translate(-50%, -50%)")
        .set("transition", "all 0.5s ease");
    circleContainer.add(shipImg);
    return shipImg;
  }

  private Div setStyleDivDirections() {
    final Div circleContainer;
    circleContainer = new Div();
    circleContainer.getStyle()
        .set("position", "relative")
        .set("width", "260px")
        .set("height", "260px")
        .set("margin", "20px auto");
    return circleContainer;
  }

  public void setAllowedDirections(List<Vec2D> forbiddenDirections) {
    Set<String> forbiddenShortNames = new HashSet<>();

    for (Vec2D vec : forbiddenDirections) {
      Directions dir = Directions.fromDelta(vec.getX(), vec.getY());
      if (dir != null) {
        forbiddenShortNames.add(dir.getShortName().toUpperCase());
      }
    }

    circleContainer.getChildren()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .forEach(button -> {

          String buttonText = button.getText().trim().toUpperCase();

          button.removeThemeVariants(
              ButtonVariant.LUMO_SUCCESS,
              ButtonVariant.LUMO_ERROR
          );

          // Ist diese Richtung verboten?
          boolean isForbidden = forbiddenShortNames.contains(buttonText);

          if (isForbidden) {
            button.addThemeVariants(ButtonVariant.LUMO_ERROR);
            button.setEnabled(false);
          } else {
            button.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            button.setEnabled(true);
          }
        });
  }

  public void resetAllDirectionsToRed() {
    circleContainer.getChildren()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .forEach(button -> {
          button.removeThemeVariants(
              ButtonVariant.LUMO_SUCCESS,
              ButtonVariant.LUMO_ERROR
          );
          button.addThemeVariants(ButtonVariant.LUMO_ERROR);
          button.setEnabled(false);
        });
  }

  private static void rotateShipOnControlPanel(Image shipImg, int dirIndex) {
    String rotation = switch (dirIndex) {
      case 0 -> "0deg";     // N
      case 1 -> "45deg";    // NE
      case 2 -> "90deg";    // E
      case 3 -> "135deg";   // SE
      case 4 -> "180deg";   // S
      case 5 -> "225deg";   // SW
      case 6 -> "270deg";   // W
      case 7 -> "315deg";   // NW
      default -> "0deg";
    };
    shipImg.getStyle()
        .set("transform", "translate(-50%, -50%) rotate(" + rotation + ")")
        .set("transform-origin", "center center");
  }

  public  void rotateShipOnSelect(Directions directions) {
    if (directions != null) {
      int index = switch (directions) {
        case Directions.NORTH_EAST -> 1;
        case Directions.EAST -> 2;
        case Directions.SOUTH_EAST -> 3;
        case Directions.SOUTH -> 4;
        case Directions.SOUTH_WEST -> 5;
        case Directions.WEST -> 6;
        case Directions.NORTH_WEST -> 7;
        default -> 0;
      };
      rotateShipOnControlPanel(shipImg, index);
      resetAllDirectionsToRed();
    }
  }

}



