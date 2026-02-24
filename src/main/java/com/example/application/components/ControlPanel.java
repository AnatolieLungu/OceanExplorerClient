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
  private H3 legendTitle;
  private VerticalLayout shipSelectionPanel;
  private Span waterLegendLabel;
  private Span deepWaterLegendLabel;
  private Span landLegendLabel;
  private Span harbourLegendLabel;
  private Span iceLegendLabel;
  private Span unknownLegendLabel;
  private Button launchBtn;
  private Button radarBtn;
  private Button scanBtn;
  private Button routeBtn;
  private Button exitBtn;
  private Button autoPilotBtn;
  private Select<String> speedSelect;
  private final Map<String, Span> shipSpanMap = new HashMap<>();

  private volatile boolean autoPilotRunning = false;
  private ExecutorService autoPilotExecutor;
  // Zusätzlicher Sync-Thread: holt Positionen periodisch aus der DB,
  // damit Bewegungen sichtbar sind, auch wenn ein AutoPilot-Request lange läuft.
  private ExecutorService autoPilotLiveSyncExecutor;

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
    shipSelectionPanel = createShipSelectionPanel();

    add(controlPanelTitle, navigationDiv);
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
    HorizontalLayout functions = new HorizontalLayout(launchBtn, radarBtn, scanBtn, routeBtn, exitBtn);
    functions.setWidthFull();
    functions.setWrap(true);
    functions.setAlignItems(FlexComponent.Alignment.CENTER);
    functions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    functions.setSpacing(true);

    stylePrimaryActionButton(launchBtn);
    stylePrimaryActionButton(radarBtn);
    stylePrimaryActionButton(scanBtn);
    stylePrimaryActionButton(routeBtn);
    stylePrimaryActionButton(exitBtn);

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
    allButtons.setWidthFull();
    return allButtons;
  }

  private VerticalLayout createShipSelectionPanel() {
    VerticalLayout wrapper = new VerticalLayout(shipsTitle, shipList, createColorLegendPanel());
    wrapper.setPadding(false);
    wrapper.setSpacing(true);
    wrapper.setWidth("260px");
    wrapper.getStyle()
        .setBackground("#f8f9fa")
        .setBorder("1px solid #ddd")
        .setBorderRadius("8px")
        .setPadding("12px");
    return wrapper;
  }

  private VerticalLayout createColorLegendPanel() {
    legendTitle = new H3(ts.get("legend.title"));
    legendTitle.getStyle()
        .setFontSize("1rem")
        .setMargin("6px 0 0 0");

    waterLegendLabel = new Span(ts.get("legend.water"));
    deepWaterLegendLabel = new Span(ts.get("legend.deepwater"));
    landLegendLabel = new Span(ts.get("legend.land"));
    harbourLegendLabel = new Span(ts.get("legend.harbour"));
    iceLegendLabel = new Span(ts.get("legend.ice"));
    unknownLegendLabel = new Span(ts.get("legend.unknown"));

    VerticalLayout legendItems = new VerticalLayout(
        createLegendItem("#6694e4ff", waterLegendLabel),
        createLegendItem("#4c6fab", deepWaterLegendLabel),
        createLegendItem("#8B4513", landLegendLabel),
        createLegendItem("#483D8B", harbourLegendLabel),
        createLegendItem("#E0FFFF", iceLegendLabel),
        createLegendItem("#2F4F4F", unknownLegendLabel)
    );
    legendItems.setPadding(false);
    legendItems.setSpacing(false);

    VerticalLayout legendPanel = new VerticalLayout(legendTitle, legendItems);
    legendPanel.setPadding(false);
    legendPanel.setSpacing(false);
    legendPanel.getStyle()
        .setBorderTop("1px solid #d9dee6")
        .setPadding("8px 0 0 0");

    return legendPanel;
  }

  private HorizontalLayout createLegendItem(String color, Span label) {
    Div swatch = new Div();
    swatch.getStyle()
        .setWidth("12px")
        .setHeight("12px")
        .setBorderRadius("3px")
        .setBorder("1px solid rgba(0,0,0,0.2)")
        .setBackground(color);

    label.getStyle()
        .setFontSize("0.78rem")
        .setColor("#334155");

    HorizontalLayout row = new HorizontalLayout(swatch, label);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setSpacing(true);
    row.getStyle().setMargin("2px 0");
    return row;
  }

  public VerticalLayout getShipSelectionPanel() {
    return shipSelectionPanel;
  }

  private void stylePrimaryActionButton(Button button) {
    button.getStyle()
        .setMinWidth("64px")
        .setPadding("0.35rem 0.55rem")
        .setFontSize("0.72rem")
        .setLineHeight("1.1");
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
        if (directionAfterNavigate == null) {
          Notification.show("Navigation failed (ship may have crashed)", 2500, Notification.Position.MIDDLE);
          refreshShipListSimple();
          return;
        }
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
      if (selectedShipData == null) {
        Notification.show("Kein Schiff ausgewählt", 2000, Notification.Position.MIDDLE);
        return;
      }
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

      // route button ist weg und muss hinzugufugt
      // ich muss auch den logic wecksln
      //

      layout.add(closeBtn);

      dialog.add(layout);
      dialog.open();

    });
  }

  private void createRouteButton() {
    routeBtn = new Button(ts.get("button.route"));

    // Wichtig: Die Logik kommt NUR in den Click-Listener
    routeBtn.addClickListener(clickEvent -> {
      if (selectedShipData == null) {
        Notification.show("Kein Schiff ausgewählt", 2000, Notification.Position.MIDDLE);
        return;
      }


      // 2. Ab hier ist selectedShipData garantiert nicht null
      List<ShipSector> shipRoute = shipService.getShipRoute(
          selectedShipData.getShipId()
      );

      // Erste Hervorhebung (grün) – sofort
      shipRoute.forEach(ship -> {
        Div cell = sea.getCell(ship.getShipSectorX(), ship.getShipSectorY());
        if (cell != null) {  // kleine Absicherung
          // Kein CSS-Border verwenden: Border kann Grid-Zellen visuell "verschieben".
          cell.getStyle()
              .setBackground("#4ad8f5")
              .set("box-shadow", "inset 0 0 0 2px #4ad8f5, inset 0 0 8px #4ad8f5");
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
                  .setBackground("#6694e4ff")
                  .set("box-shadow", "inset 0 0 0 2px #6694e4ff, inset 0 0 8px #6694e4ff");
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

      ShipData shipToRemove = selectedShipData;

      if (autoPilotRunning) {
        stopAutoPilot();
      }

      shipService.exit(shipToRemove.getShipId());
      sea.removeShipFromSea(shipToRemove);

      // Re-sync UI list from backend; removed ship must not be re-added locally.
      refreshShipListSimple();
      navigation.resetAllDirectionsToRed();

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
    String autoPilotShipId = selectedShipData.getShipId();
    if (autoPilotShipId == null || autoPilotShipId.isBlank()) {
      Notification.show("Invalid ship selection", 2000, Notification.Position.MIDDLE);
      return;
    }

    autoPilotRunning = true;
    styleAutoPilotButton(true);
    setControlsEnabled(false);

    UI ui = UI.getCurrent();
    if (ui == null) return;

    // Live-Sync parallel starten: UI zeigt Zwischenpositionen in nahezu Echtzeit.

    startAutoPilotLiveSync(ui, autoPilotShipId);

    autoPilotExecutor = Executors.newSingleThreadExecutor();
    autoPilotExecutor.submit(() -> {
      try {
        while (autoPilotRunning) {
          AutoPilotData data = shipService.runAutoPilotStep(autoPilotShipId);

          ui.access(() -> {
            ShipData currentShip = findShipById(autoPilotShipId);
            if (currentShip != null && data != null) {
              sea.applyAutoPilotStep(currentShip, data);
              Span span = shipSpanMap.get(autoPilotShipId);
              if (span != null) {
                span.setText(buildShipInfoText(currentShip));
              }
            }
          });

          Thread.sleep(getDelayFromSpeed());
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        if (!autoPilotRunning || isInterruption(ex)) {
          return;
        }
        ui.access(() -> {
          Notification.show(ts.get("autopilot.error") + ": " + ex.getMessage(),
              3000, Notification.Position.MIDDLE);
          stopAutoPilot();
        });
      }
    });

    Notification.show(ts.get("autopilot.started"), 2000, Notification.Position.BOTTOM_START);
  }

  private void startAutoPilotLiveSync(UI ui, String shipId) {
    autoPilotLiveSyncExecutor = Executors.newSingleThreadExecutor();
    autoPilotLiveSyncExecutor.submit(() -> {
      try {
        while (autoPilotRunning) {
          // Position + Kartenfarben gemeinsam aktualisieren, damit Wasser-Felder nicht hinterherhinken.
          List<ShipData> latestShips = shipService.getShips();
          List<SectorInfo> latestMap = shipService.loadMap();
          ui.access(() -> {
            sea.applyMapSectors(latestMap);
            applyLiveShipSnapshot(shipId, latestShips);
          });
          Thread.sleep(200);
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        if (!autoPilotRunning || isInterruption(ex)) {
          return;
        }
        ui.access(() -> {
          Notification.show("Autopilot sync error: " + ex.getMessage(),
              2500, Notification.Position.BOTTOM_START);
          stopAutoPilot();
        });
      }
    });
  }

  private void applyLiveShipSnapshot(String shipId, List<ShipData> latestShips) {
    if (!autoPilotRunning || latestShips == null) {
      return;
    }
    allShipData = latestShips;

    ShipData latest = null;
    for (ShipData ship : latestShips) {
      if (shipId.equals(ship.getShipId())) {
        latest = ship;
        break;
      }
    }

    // Wenn das Schiff nicht mehr vorhanden ist, AutoPilot sauber stoppen.
    if (latest == null) {
      stopAutoPilot();
      return;
    }

    if (selectedShipData == null || !shipId.equals(selectedShipData.getShipId())) {
      selectedShipData = latest;
    } else {
      selectedShipData.setSectorX(latest.getSectorX());
      selectedShipData.setSectorY(latest.getSectorY());
      selectedShipData.setDirectionX(latest.getDirectionX());
      selectedShipData.setDirectionY(latest.getDirectionY());
      selectedShipData.setShipName(latest.getShipName());
    }

    // Nur die visuelle Position aktualisieren (ohne kompletten Listen-Reload).
    sea.placeShipOnSea(selectedShipData);
    Span span = shipSpanMap.get(shipId);
    if (span != null) {
      span.setText(buildShipInfoText(selectedShipData));
    }
    navigation.rotateShipOnSelect(
        Directions.fromDelta(selectedShipData.getDirectionX(), selectedShipData.getDirectionY()));
  }

  private ShipData findShipById(String shipId) {
    if (shipId == null || shipId.isBlank()) {
      return null;
    }
    for (ShipData ship : allShipData) {
      if (shipId.equals(ship.getShipId())) {
        return ship;
      }
    }
    return null;
  }

  private boolean isInterruption(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof InterruptedException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void stopAutoPilot() {
    if (!autoPilotRunning) return;

    autoPilotRunning = false;
    if (autoPilotExecutor != null) {
      autoPilotExecutor.shutdownNow();
      autoPilotExecutor = null;
    }
    if (autoPilotLiveSyncExecutor != null) {
      autoPilotLiveSyncExecutor.shutdownNow();
      autoPilotLiveSyncExecutor = null;
    }
    styleAutoPilotButton(false);
    setControlsEnabled(true);

    Notification.show(ts.get("autopilot.stopped"), 2000, Notification.Position.BOTTOM_START);
  }

  private long getDelayFromSpeed() {
    if (speedSelect == null) return 500;
    return switch (speedSelect.getValue()) {
      case "Slow" -> 500;
      case "Fast" -> 5;
      default -> 60;
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
    routeBtn.setEnabled(enabled);
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
    routeBtn.setText(ts.get("button.route"));
    exitBtn.setText(ts.get("button.exit"));
    styleAutoPilotButton(autoPilotRunning);
    if (legendTitle != null) {
      legendTitle.setText(ts.get("legend.title"));
      waterLegendLabel.setText(ts.get("legend.water"));
      deepWaterLegendLabel.setText(ts.get("legend.deepwater"));
      landLegendLabel.setText(ts.get("legend.land"));
      harbourLegendLabel.setText(ts.get("legend.harbour"));
      iceLegendLabel.setText(ts.get("legend.ice"));
      unknownLegendLabel.setText(ts.get("legend.unknown"));
    }

    for (ShipData shipData : allShipData) {
      Span span = shipSpanMap.get(shipData.getShipId());
      if (span != null) {
        span.setText(buildShipInfoText(shipData));
      }
    }
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
    selectedShipData = shipData;
    refreshShipListSimple();
  }

  private void addShipToControlPanel(ShipData shipData) {

    HorizontalLayout itemLayout = getShipContainerLayout(shipData);
    itemLayout.getElement().setProperty("ship-id", shipData.getShipId());
    itemLayout.addClickListener(e -> {
      selectedShipData = shipData;
      highlightSelectedShip();
      navigation.rotateShipOnSelect(Directions.fromDelta(selectedShipData.getDirectionX(), selectedShipData.getDirectionY()));
    });
    if (selectedShipData != null && Objects.equals(selectedShipData.getShipId(), shipData.getShipId())) {
      itemLayout.getElement().getStyle().setBackground("#c3e0ff");
    }
    shipList.addComponentAsFirst(itemLayout);
  }

  private void highlightSelectedShip() {
    String selectedShipId = selectedShipData != null ? selectedShipData.getShipId() : null;
    shipList.getChildren().forEach(c -> {
      String rowShipId = c.getElement().getProperty("ship-id");
      if (selectedShipId != null && selectedShipId.equals(rowShipId)) {
        c.getElement().getStyle().setBackground("#c3e0ff");
      } else {
        c.getElement().getStyle().setBackground("#f0f4f8");
      }
    });
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
    shipSpanMap.put(shipData.getShipId(), text);

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
    String selectedShipId = selectedShipData != null ? selectedShipData.getShipId() : null;

    shipList.removeAll();           // alles weg
    shipSpanMap.clear();            // Map leeren, sonst Speicher-Leak + alte Objekte

    allShipData = shipService.getShips();   // aktuelle Daten holen

    selectedShipData = null;
    for (ShipData ship : allShipData) {
      addShipToControlPanel(ship);    // jedes Schiff neu hinzufügen
      if (selectedShipId != null && selectedShipId.equals(ship.getShipId())) {
        selectedShipData = ship;
      }
    }


    if (selectedShipData == null) {
      navigation.resetAllDirectionsToRed();
    } else {
      Directions selectedDir = Directions.fromDelta(selectedShipData.getDirectionX(), selectedShipData.getDirectionY());
      navigation.rotateShipOnSelect(selectedDir);
      highlightSelectedShip();
    }
  }

}
