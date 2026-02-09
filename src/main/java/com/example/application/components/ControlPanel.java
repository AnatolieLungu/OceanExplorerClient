package com.example.application.components;

import com.example.application.entity.Directions;
import com.example.application.entity.ShipData;
import com.example.application.entity.Vec2D;

import com.example.application.service.ShipCommandService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@SpringComponent
@UIScope
public class ControlPanel extends Div {

  private final ShipCommandService shipService;
  private final Sea sea;
  private final VerticalLayout shipList = new VerticalLayout();
  private ShipData selectedShipData;


  @PostConstruct
  public void init(){
    setupShipList();
  }

  @Autowired
  public ControlPanel(Sea sea, ShipCommandService shipService, Navigation navigation) {
    this.shipService = shipService;

    this.sea = sea;

    setWidth("350px");
    setHeight("685px");
    getStyle()
        .setBackground("#f8f9fa")
        .setBorder("1px solid #ddd")
        .setBorderRadius("8px")
        .setPadding("16px");

    H3 title = new H3("Control Panel");
    title.getStyle().setTextAlign(Style.TextAlign.CENTER);
    add(title);



    navigation.setDirectionListener(selectedDirection -> {
      Directions navigableDirection = Directions.fromShortName(selectedDirection);

      if (selectedShipData != null) {
        Directions actualDirection = Directions.fromDelta(selectedShipData.getDirectionX(),selectedShipData.getDirectionY());
        List<Vec2D> unavailableDirections = shipService.getUnavailableDirections(selectedShipData);
        shipService.navigate(selectedShipData.getShipId(),actualDirection,navigableDirection);
        sea.moveShip(selectedShipData, navigableDirection);
        navigation.setAllowedDirections(unavailableDirections);
      }
    });

    navigation.resetAllDirectionsToRed();

    HorizontalLayout functions = new HorizontalLayout(
        new Button("LAUNCH", e -> openAddShipDialog()),
      //  new Button("RADAR", e -> navigation.setAllowedDirections(Navigation.getInitialDirectionsAllowed())),
        new Button("RADAR", e -> {
          navigation.setAllowedDirections(shipService.getUnavailableDirections(selectedShipData));
          shipService.getUnavailableDirections(selectedShipData);
        }),
        new Button("SCAN"),
        new Button("EXIT", e -> {
          shipService.exit(selectedShipData.getShipId());
          shipList.getChildren()
              .filter(c -> c instanceof HorizontalLayout)
              .map(c -> (HorizontalLayout) c)
              .filter(layout -> {
                // Schau, ob der Span-Text den Namen oder shipId enthält
                return layout.getChildren()
                    .filter(child -> child instanceof Span)
                    .map(child -> (Span) child)
                    .anyMatch(span -> span.getText().contains(selectedShipData.getShipId()) || span.getText().contains(selectedShipData.getName()));
              })
              .findFirst()
              .ifPresent(shipList::remove);

          selectedShipData = null;
        })
    );

    functions.setAlignItems(FlexComponent.Alignment.CENTER);
    functions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    functions.setSpacing(true);

    VerticalLayout controls = new VerticalLayout(
        navigation,
        functions
    );

    controls.setPadding(true);
    controls.setSpacing(true);
    controls.setAlignItems(FlexComponent.Alignment.STRETCH);

    H3 shipsTitle = new H3("Schiffe");
    shipsTitle.getStyle().setFontSize("1.2rem");
    shipsTitle.getStyle().setTextAlign(Style.TextAlign.CENTER);

    add(title, controls, shipsTitle, shipList);
  }

  private void addShip(String name, int x, int y, String directionShortName) {
    Directions direction = Directions.fromShortName(directionShortName);
    ShipData shipData = new ShipData(null, name, x, y, direction.getDx(),direction.getDy());
    String response = shipService.launch(shipData.getName(),shipData.getSectorX(),shipData.getSectorY(),shipData.getDirectionX(),shipData.getDirectionY());

    if (response.equals("Error")) {
      Notification.show("🚫 Schiff konntre nicht gestartet werden");
      return;
    }
    shipData.setShipId(response);
    Notification.show("Schiff »" + name + "« erfolgreich gestartet!", 2000, Notification.Position.BOTTOM_START);
    sea.placeShipOnSea(shipData);
    addShipToControlPanel(shipData);
  }

  private void addShipToControlPanel(ShipData shipData) {
    HorizontalLayout itemLayout = getShipContainerLayout(shipData);
    itemLayout.addClickListener(e -> {
      shipList.getChildren().forEach(c ->
          c.getElement().getStyle().setBackground("#f0f4f8"));
      itemLayout.getElement().getStyle().setBackground("#c3e0ff");
      selectedShipData = shipData;
    });
    shipList.add(itemLayout);
  }

  private static HorizontalLayout getShipContainerLayout(ShipData shipData) {
    HorizontalLayout itemLayout = new HorizontalLayout();
    itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    itemLayout.setPadding(false);
    itemLayout.setSpacing(true);
    itemLayout.getStyle()
        .setBackground("#f0f4f8")
        .setMargin("4px 0")
        .setPadding("6px 10px")
        .setBorderRadius("6px")
        .setCursor("pointer")
        .set("width", "100%");

    // Bild hinzufügen
    Image shipIcon = new Image("images/ship.png", "Ship icon");
    shipIcon.setWidth("34px");
    shipIcon.setHeight("34px");

    Span text = new Span(
        shipData.getName() + " sector: " + "(" + shipData.getSectorX() + "," + shipData.getSectorY() + ")" + " direction: " + Directions.fromDelta(
            shipData.getDirectionX(), shipData.getDirectionY()));
    text.getStyle().set("font-size", "0.95rem");

    itemLayout.add(shipIcon, text);
    return itemLayout;
  }



  private void openAddShipDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Neues Schiff");

    TextField nameField = new TextField("Name");
    nameField.setPlaceholder("z.B. Boot 1");
    nameField.setRequired(true);
    nameField.setWidthFull();

    TextField xField = new TextField("X-Koordinate");
    xField.setPlaceholder("z. B. 20");
    xField.setRequiredIndicatorVisible(true);
    xField.setAllowedCharPattern("[0-9]");
    xField.setMaxLength(2);
    xField.setPattern("[0-9]{0,2}");
    xField.setRequired(true);

    TextField yField = new TextField("Y-Koordinate");
    yField.setPlaceholder("z. B. 20");
    yField.setRequiredIndicatorVisible(true);
    yField.setAllowedCharPattern("[0-9]");
    yField.setMaxLength(2);
    yField.setPattern("[0-9]{0,2}");
    yField.setRequired(true);

    Select<String> directionSelect = new Select<>();
    directionSelect.setLabel("Richtung");
    directionSelect.setItems("N", "NE", "E", "SE", "S", "SW", "W", "NW");
    directionSelect.setValue("N");
    directionSelect.setWidthFull();

    Button save = new Button("Hinzufügen", evt -> {
      try {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
          nameField.setInvalid(true);
          return;
        }

        int x = Integer.parseInt(xField.getValue().replace(",", "."));
        int y = Integer.parseInt(yField.getValue().replace(",", "."));

        addShip(name, x, y, directionSelect.getValue());
        dialog.close();
      } catch (NumberFormatException ex) {
        xField.setInvalid(true);
        yField.setInvalid(true);
      }
    });

    Button cancel = new Button("Abbrechen", e -> dialog.close());

    HorizontalLayout buttons = new HorizontalLayout(save, cancel);
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    VerticalLayout layout = new VerticalLayout(
        nameField, xField, yField, directionSelect, buttons
    );
    layout.setPadding(true);
    layout.setSpacing(true);

    dialog.add(layout);
    dialog.open();
  }

  private void setupShipList() {
    shipList.setPadding(false);
    shipList.setSpacing(false);

    // Wichtigste Zeilen für Scroll:
    shipList.setHeight("200px");           // ← fixe Höhe von Anfang an
    shipList.setMaxHeight("200px");           // ← Hier kannst du die Höhe anpassen
    shipList.getStyle()
        .set("overflow-y", "auto")            // vertikaler Scroll wenn nötig
        .set("overflow-x", "hidden")
        .set("border", "1px solid #e0e0e0")   // optional: schöner Rahmen
        .set("border-radius", "6px")
        .setBackground("#ffffff");            // optional: weißer Hintergrund

    // Optional: Mindesthöhe, damit es auch bei wenigen Schiffen schön aussieht
    shipList.setMinHeight("120px");

    List<ShipData> allShipData = shipService.getShips();

    for (ShipData shipData : allShipData) {
      addShipToControlPanel(shipData);

      // Wichtig: Auch auf der Karte anzeigen!
      sea.placeShipOnSea(shipData);
    }
  }



}
