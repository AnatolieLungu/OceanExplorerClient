package com.example.application.components;

import com.example.application.entity.*;
import com.example.application.i18n.TranslationService;
import com.example.application.service.ShipCommandService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringComponent
@UIScope
public class ControlPanel extends Div {

  private final ShipCommandService shipService;
  private final TranslationService ts;
  private final Sea sea;

  private final VerticalLayout shipList = new VerticalLayout();
  private ShipData selectedShipData;
  private final Navigation navigation;
  private List<ShipData> allShipData = new ArrayList<>();


  private H3 controlPanelTitle;
  private H3 shipsTitle;
  private Button launchBtn;
  private Button radarBtn;
  private Button scanBtn;
  private Button exitBtn;
  private Button autoPilotBtn;
  private Select<String> speedSelect;
  private final Map<ShipData, Span> shipSpanMap = new HashMap<>();

  private volatile boolean autoPilotRunning = false;
  private ExecutorService autoPilotExecutor;

  @PostConstruct
  public void init() {
    setupShipList();
    if (!allShipData.isEmpty()) {
      selectedShipData = allShipData.getFirst();
    }
    ts.addLanguageChangeListener(this::updateTexts);

    addDetachListener(e -> stopAutoPilot());
  }

  @Autowired
  public ControlPanel(Sea sea, ShipCommandService shipService, Navigation navigation,
                      TranslationService translationService) {
    this.shipService = shipService;
    this.sea = sea;
    this.ts = translationService;
    this.navigation = navigation;

    setDivStyle();

    createShipNavigationComponent(sea, shipService, navigation);

    VerticalLayout navigationDiv = createnNavigationDiv(navigation);

    setShipListTitle();

    add(controlPanelTitle, navigationDiv, shipsTitle, shipList);
  }

  private void setShipListTitle() {
    shipsTitle = new H3(ts.get("ships.title"));
    shipsTitle.getStyle().setFontSize("1.2rem");
    shipsTitle.getStyle().setTextAlign(Style.TextAlign.CENTER);
  }

  private VerticalLayout createnNavigationDiv(Navigation navigation) {

    VerticalLayout controllerButtons = createControllerButtons();

    VerticalLayout controls = new VerticalLayout(
        navigation,
        controllerButtons
    );

    controls.setPadding(true);
    controls.setSpacing(true);
    controls.setAlignItems(FlexComponent.Alignment.STRETCH);
    return controls;
  }

  private VerticalLayout createControllerButtons() {
    HorizontalLayout functions = new HorizontalLayout(launchBtn, radarBtn, scanBtn, exitBtn);
    functions.setAlignItems(FlexComponent.Alignment.CENTER);
    functions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    functions.setSpacing(true);

    speedSelect = new Select<>();
    speedSelect.setItems("Slow", "Normal", "Fast");
    speedSelect.setValue("Normal");
    speedSelect.setWidth("100px");
    speedSelect.getStyle().setFontSize("0.8rem");

    HorizontalLayout autoPilotRow = new HorizontalLayout(autoPilotBtn, speedSelect);
    autoPilotRow.setAlignItems(FlexComponent.Alignment.CENTER);
    autoPilotRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    autoPilotRow.setSpacing(true);

    VerticalLayout allButtons = new VerticalLayout(functions, autoPilotRow);
    allButtons.setPadding(false);
    allButtons.setSpacing(true);
    allButtons.setAlignItems(FlexComponent.Alignment.CENTER);
    return allButtons;
  }

  private void createShipNavigationComponent(Sea sea, ShipCommandService shipService, Navigation navigation) {
    controlPanelTitle = new H3(ts.get("control.panel.title"));
    controlPanelTitle.getStyle().setTextAlign(Style.TextAlign.CENTER);
    //Navigation on Control Panel
    navigation.setDirectionListener(selectedDirection -> {

      Directions navigableDirection = Directions.fromShortName(selectedDirection);


      if (selectedShipData != null) {
        Directions actualDirection = Directions.fromDelta(
            selectedShipData.getDirectionX(), selectedShipData.getDirectionY());

        Vec2D directionAfterNavigate = shipService.navigate(selectedShipData.getShipId(), actualDirection, navigableDirection);
        System.out.println("actualDirection:  " + actualDirection.toString());
        System.out.println("navigableDirection: " + navigableDirection.toString());
        sea.moveShip(selectedShipData, navigableDirection,Directions.fromDelta(directionAfterNavigate.getX(), directionAfterNavigate.getY()));

        selectedShipData.setDirectionX(directionAfterNavigate.getX());
        selectedShipData.setDirectionY(directionAfterNavigate.getY());

        //shiff bewegt sich richtig
        navigation.rotateShipOnSelect(Directions.fromDelta(directionAfterNavigate.getX(), directionAfterNavigate.getY()));

        List<Vec2D> unavailableDirections = shipService.getUnavailableDirections(selectedShipData);


        navigation.setAllowedDirections(unavailableDirections);
        refreshShipListSimple();
      }
    });

    navigation.resetAllDirectionsToRed();
    //Create funtional buttons
    createFunctionsButtons(sea, shipService, navigation);
  }

  private void createFunctionsButtons(Sea sea, ShipCommandService shipService, Navigation navigation) {
    createLaunchButton();
    createRadarButton(shipService, navigation);
    createScanButton();
    createRouteButton();
    createExitButton(sea, shipService, navigation);
    createAutoPilotButton();
  }

  private void createLaunchButton() {
    launchBtn = new Button(ts.get("button.launch"), e -> openAddShipDialog());
  }

  private void createRadarButton(ShipCommandService shipService, Navigation navigation) {
    radarBtn = new Button(ts.get("button.radar"), e -> {
      navigation.setAllowedDirections(shipService.getUnavailableDirections(selectedShipData));

      List<Echo> echoes = shipService.getSectorInfo(selectedShipData.getShipId());

      for (Echo echo : echoes) {
        Sector sector = echo.getSector();
        if (sector == null) continue;

        int x = sector.getVec2()[0];
        int y = sector.getVec2()[1];

        if (x < 0 || x >= 99 || y < 0 || y >= 99) continue;

        Div cell = sea.getCell(x, y);
        if (cell == null) continue;
        if (echo.getGround().equals(Ground.Land)){
          cell.getStyle()
              .setBorder("2px solid #46c946")
              .setBackground("#46c946")
              .set("box-shadow", "inset 0 0 8px #46c946");
        }else {
          cell.getStyle()
              .setBorder("1px solid #6694e4ff")
              .setBackground("#6694e4ff")
              .set("box-shadow", "inset 0 0 8px #6694e4ff");
        }
      }
    });
  }

  private void createScanButton() {
    scanBtn = new Button(ts.get("button.scan"),e ->{
      if (selectedShipData == null) {
        Notification.show("Kein Schiff ausgewählt", 2000, Notification.Position.MIDDLE);
        return;
      }

      Dialog dialog = new Dialog();
      dialog.setHeaderTitle("Sector-Info");
      dialog.setCloseOnEsc(true);
      dialog.setCloseOnOutsideClick(true);
      dialog.setWidth("340px");

      VerticalLayout layout = new VerticalLayout();
      layout.setPadding(true);
      layout.setSpacing(true);
      ScanResult scanResult = shipService.scan(selectedShipData.getShipId());

      // Einzelne Felder (Label + Wert)
      layout.add(createField("Shiff : ", selectedShipData.getShipName()));
      layout.add(createField("Sector",
          "(" + selectedShipData.getSectorX() + ", " + selectedShipData.getSectorY() + ")"));
      layout.add(createField("Tiefe", String.valueOf(scanResult.getDepth())));
      layout.add(createField("Ábweichung", String.valueOf(scanResult.getStddev())));

      // Close-Button
      Button closeBtn = new Button("Schließen", ev -> dialog.close());
      closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      closeBtn.getStyle().setMarginTop("16px");

      layout.add(closeBtn);

      dialog.add(layout);
      dialog.open();

    });
  }

  private void createRouteButton() {
    scanBtn = new Button(ts.get("button.route"));

    // Wichtig: Die Logik kommt NUR in den Click-Listener
    scanBtn.addClickListener(clickEvent -> {


      // 2. Ab hier ist selectedShipData garantiert nicht null
      List<ShipSector> shipRoute = shipService.getShipRoute(
          selectedShipData.getShipId()
      );

      // Erste Hervorhebung (grün) – sofort
      shipRoute.forEach(ship -> {
        Div cell = sea.getCell(ship.getShipSectorX(), ship.getShipSectorY());
        if (cell != null) {  // kleine Absicherung
          cell.getStyle()
              .setBorder("2px solid #4ad8f5")
              .setBackground("#4ad8f5")
              .set("box-shadow", "inset 0 0 8px #4ad8f5");
        }
      });

      // 3 Sekunden Pause + zweite Hervorhebung (blau)
      UI ui = UI.getCurrent();
      if (ui == null) return;

      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        ui.access(() -> {
          shipRoute.forEach(ship -> {
            Div cell = sea.getCell(ship.getShipSectorX(), ship.getShipSectorY());
            if (cell != null) {
              cell.getStyle()
                  .setBorder("2px solid #6694e4ff")
                  .setBackground("#6694e4ff")
                  .set("box-shadow", "inset 0 0 8px #6694e4ff");
            }
          });
        });
      });
    });
  }

  private HorizontalLayout createField(String labelText, String value) {
    Span label = new Span(labelText + ":");
    label.getStyle()
        .setFontWeight("500")
        .setMinWidth("90px")
        .setColor("#555");

    Span valueSpan = new Span(value);
    valueSpan.getStyle()
        .setFontWeight("bold")
        .setColor("#0066cc");

    HorizontalLayout row = new HorizontalLayout(label, valueSpan);
    row.setWidthFull();
    row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    row.setAlignItems(FlexComponent.Alignment.BASELINE);
    row.setSpacing(true);

    return row;
  }

  private void createExitButton(Sea sea, ShipCommandService shipService, Navigation navigation) {
    exitBtn = new Button(ts.get("button.exit"), e -> {
      if (selectedShipData == null) {
        return;
      }

      shipService.exit(selectedShipData.getShipId());

      Span span = shipSpanMap.remove(selectedShipData);
      if (span != null) {
        span.getParent().ifPresent(shipList::remove);
      }

      sea.removeShipFromSea(selectedShipData);

      navigation.resetAllDirectionsToRed();
      addShipToControlPanel(selectedShipData);

    });
  }

  private void createAutoPilotButton() {
    autoPilotBtn = new Button(ts.get("button.autopilot"), e -> {
      if (autoPilotRunning) {
        stopAutoPilot();
      } else {
        startAutoPilot();
      }
    });
    styleAutoPilotButton(false);
  }

  private void startAutoPilot() {
    if (selectedShipData == null) {
      Notification.show("No ship selected", 2000, Notification.Position.MIDDLE);
      return;
    }

    autoPilotRunning = true;
    styleAutoPilotButton(true);
    setControlsEnabled(false);

    UI ui = UI.getCurrent();
    if (ui == null) return;

    autoPilotExecutor = Executors.newSingleThreadExecutor();
    autoPilotExecutor.submit(() -> {
      try {
        while (autoPilotRunning) {
          AutoPilotData data = shipService.runAutoPilotStep(selectedShipData.getShipId());

          ui.access(() -> {
            sea.applyAutoPilotStep(selectedShipData, data);
            refreshShipListSimple();
            if (data.getShipPosition() != null) {
              Directions dir = Directions.fromDelta(
                  selectedShipData.getDirectionX(), selectedShipData.getDirectionY());
              navigation.rotateShipOnSelect(dir);
            }
          });

          Thread.sleep(getDelayFromSpeed());
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        ui.access(() -> {
          Notification.show(ts.get("autopilot.error") + ": " + ex.getMessage(),
              3000, Notification.Position.MIDDLE);
          stopAutoPilot();
        });
      }
    });

    Notification.show(ts.get("autopilot.started"), 2000, Notification.Position.BOTTOM_START);
  }

  private void stopAutoPilot() {
    if (!autoPilotRunning) return;

    autoPilotRunning = false;
    if (autoPilotExecutor != null) {
      autoPilotExecutor.shutdownNow();
      autoPilotExecutor = null;
    }
    styleAutoPilotButton(false);
    setControlsEnabled(true);

    Notification.show(ts.get("autopilot.stopped"), 2000, Notification.Position.BOTTOM_START);
  }

  private long getDelayFromSpeed() {
    if (speedSelect == null) return 500;
    return switch (speedSelect.getValue()) {
      case "Slow" -> 1000;
      case "Fast" -> 200;
      default -> 500;
    };
  }

  private void styleAutoPilotButton(boolean running) {
    if (running) {
      autoPilotBtn.setText(ts.get("button.autopilot.stop"));
      autoPilotBtn.getStyle()
          .setBackground("#e74c3c")
          .setColor("white")
          .set("font-weight", "bold");
    } else {
      autoPilotBtn.setText(ts.get("button.autopilot"));
      autoPilotBtn.getStyle()
          .setBackground("#0066cc")
          .setColor("white")
          .set("font-weight", "bold");
    }
  }

  private void setControlsEnabled(boolean enabled) {
    launchBtn.setEnabled(enabled);
    radarBtn.setEnabled(enabled);
    scanBtn.setEnabled(enabled);
    exitBtn.setEnabled(enabled);
    navigation.setEnabled(enabled);
  }

  private void setDivStyle() {
    setWidth("350px");
    setHeight("685px");
    getStyle()
        .setBackground("#f8f9fa")
        .setBorder("1px solid #ddd")
        .setBorderRadius("8px")
        .setPadding("16px")
        .setDisplay(Style.Display.FLEX)
        .set("flex-direction", "column")
        .setOverflow(Style.Overflow.HIDDEN);
  }

  private void updateTexts() {
    controlPanelTitle.setText(ts.get("control.panel.title"));
    shipsTitle.setText(ts.get("ships.title"));
    launchBtn.setText(ts.get("button.launch"));
    radarBtn.setText(ts.get("button.radar"));
    scanBtn.setText(ts.get("button.scan"));
    exitBtn.setText(ts.get("button.exit"));
    styleAutoPilotButton(autoPilotRunning);

    shipSpanMap.forEach((shipData, span) ->
        span.setText(buildShipInfoText(shipData)));
    }

  private String buildShipInfoText(ShipData shipData) {
    return shipData.getShipName()
        + " " + ts.get("ship.info.sector") + ": "
        + "(" + shipData.getSectorX() + "," + shipData.getSectorY() + ")"
        + " " + ts.get("ship.info.direction") + ": "
        + Directions.nameFromDirection(Directions.fromDelta(shipData.getDirectionX(), shipData.getDirectionY()));
  }

  private void addShip(String name, int x, int y, String directionShortName) {
    Directions direction = Directions.fromShortName(directionShortName);
    ShipData shipData = new ShipData(null, name, x, y, direction.getDx(), direction.getDy());
    String response = shipService.launch(
        shipData.getShipName(), shipData.getSectorX(), shipData.getSectorY(),
        shipData.getDirectionX(), shipData.getDirectionY());


    if (response == null || !response.contains("#")) {
      System.out.println(response);
      String errorMsg = response != null ? response : ts.get("error.unknown");
      Notification.show("\uD83D\uDEAB " + errorMsg);
      return;
    }
    shipData.setShipId(response);
    Notification.show(ts.get("ship.launched", name), 2000, Notification.Position.BOTTOM_START);
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
      navigation.rotateShipOnSelect(Directions.fromDelta(selectedShipData.getDirectionX(), selectedShipData.getDirectionY()));
    });
    shipList.addComponentAsFirst(itemLayout);
  }

  private HorizontalLayout getShipContainerLayout(ShipData shipData) {
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

    Image shipIcon = new Image("images/ship.png", "Ship icon");
    shipIcon.setWidth("34px");
    shipIcon.setHeight("34px");

    Span text = new Span(buildShipInfoText(shipData));
    text.getStyle().set("font-size", "0.95rem");
    shipSpanMap.put(shipData, text);

    itemLayout.add(shipIcon, text);
    return itemLayout;
  }

  private void openAddShipDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(ts.get("dialog.new.ship"));

    TextField nameField = new TextField(ts.get("field.name"));
    nameField.setPlaceholder(ts.get("field.name.placeholder"));
    nameField.setRequired(true);
    nameField.setWidthFull();

    TextField xField = new TextField(ts.get("field.x.coordinate"));
    xField.setPlaceholder(ts.get("field.x.placeholder"));
    xField.setRequiredIndicatorVisible(true);
    xField.setAllowedCharPattern("[0-9]");
    xField.setMaxLength(2);
    xField.setPattern("[0-9]{0,2}");
    xField.setRequired(true);

    TextField yField = new TextField(ts.get("field.y.coordinate"));
    yField.setPlaceholder(ts.get("field.y.placeholder"));
    yField.setRequiredIndicatorVisible(true);
    yField.setAllowedCharPattern("[0-9]");
    yField.setMaxLength(2);
    yField.setPattern("[0-9]{0,2}");
    yField.setRequired(true);

    Select<String> directionSelect = new Select<>();
    directionSelect.setLabel(ts.get("field.direction"));
    directionSelect.setItems("N", "NE", "E", "SE", "S", "SW", "W", "NW");
    directionSelect.setValue("N");
    directionSelect.setWidthFull();

    Button save = new Button(ts.get("button.add"), evt -> {
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

    Button cancel = new Button(ts.get("button.cancel"), e -> dialog.close());

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

    shipList.getStyle()
        .set("flex", "1 1 auto")
        .set("overflow-y", "auto")
        .set("overflow-x", "hidden")
        .set("border", "1px solid #e0e0e0")
        .set("border-radius", "6px")
        .setBackground("#ffffff")
        .setMinHeight("0");

    allShipData = shipService.getShips();

    for (ShipData shipData : allShipData) {
      addShipToControlPanel(shipData);
      sea.placeShipOnSea(shipData);
    }
  }

  // 1. Neue Methode (oder ersetze die alte refresh-Methode)
  private void refreshShipListSimple() {
    shipList.removeAll();           // alles weg
    shipSpanMap.clear();            // Map leeren, sonst Speicher-Leak + alte Objekte

    allShipData = shipService.getShips();   // aktuelle Daten holen

    for (ShipData ship : allShipData) {
      addShipToControlPanel(ship);    // jedes Schiff neu hinzufügen
    }


    navigation.resetAllDirectionsToRed();   // Navigation zurücksetzen, weil nichts mehr ausgewählt
  }

}
