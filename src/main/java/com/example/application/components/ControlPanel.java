package com.example.application.components;

import com.example.application.entity.Directions;
import com.example.application.entity.ShipData;
import com.example.application.entity.Vec2D;
import com.example.application.i18n.TranslationService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


  // Translatable UI components (updated on language change)
  private H3 controlPanelTitle;
  private H3 shipsTitle;
  private Button launchBtn;
  private Button radarBtn;
  private Button scanBtn;
  private Button exitBtn;
  private final Map<ShipData, Span> shipSpanMap = new HashMap<>();

  @PostConstruct
  public void init() {
    setupShipList();
    ts.addLanguageChangeListener(this::updateTexts);
  }

  @Autowired
  public ControlPanel(Sea sea, ShipCommandService shipService, Navigation navigation,
                      TranslationService translationService) {
    this.shipService = shipService;
    this.sea = sea;
    this.ts = translationService;
    this.navigation = navigation;

    setWidth("350px");
    setHeight("685px");
    getStyle()
        .setBackground("#f8f9fa")
        .setBorder("1px solid #ddd")
        .setBorderRadius("8px")
        .setPadding("16px");

    controlPanelTitle = new H3(ts.get("control.panel.title"));
    controlPanelTitle.getStyle().setTextAlign(Style.TextAlign.CENTER);

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
        navigation.setupShipOnControlPanel(Directions.fromDelta(directionAfterNavigate.getX(), directionAfterNavigate.getY()));

        List<Vec2D> unavailableDirections = shipService.getUnavailableDirections(selectedShipData);


        navigation.setAllowedDirections(unavailableDirections);
        refreshShipList();

      }
    });

    navigation.resetAllDirectionsToRed();

    launchBtn = new Button(ts.get("button.launch"), e -> openAddShipDialog());
    radarBtn = new Button(ts.get("button.radar"), e -> {
      navigation.setAllowedDirections(shipService.getUnavailableDirections(selectedShipData));
    });
    scanBtn = new Button(ts.get("button.scan"));
    exitBtn = new Button(ts.get("button.exit"), e -> {
      if (selectedShipData == null) {
        return;
      }

      // Tell the backend
      shipService.exit(selectedShipData.getShipId());

      // Remove ship from the control-panel list
      Span span = shipSpanMap.remove(selectedShipData);
      if (span != null) {
        span.getParent().ifPresent(parent -> shipList.remove(parent));
      }

      // Remove ship icon from the sea grid
      sea.removeShipFromSea(selectedShipData);

      // Reset navigation buttons (no ship selected)
      navigation.resetAllDirectionsToRed();
      addShipToControlPanel(selectedShipData);
      selectedShipData = null;
    });

    HorizontalLayout functions = new HorizontalLayout(launchBtn, radarBtn, scanBtn, exitBtn);
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

    shipsTitle = new H3(ts.get("ships.title"));
    shipsTitle.getStyle().setFontSize("1.2rem");
    shipsTitle.getStyle().setTextAlign(Style.TextAlign.CENTER);

    add(controlPanelTitle, controls, shipsTitle, shipList);
  }

  /** Called when the language changes – refreshes every translatable text. */
  private void updateTexts() {
    controlPanelTitle.setText(ts.get("control.panel.title"));
    shipsTitle.setText(ts.get("ships.title"));
    launchBtn.setText(ts.get("button.launch"));
    radarBtn.setText(ts.get("button.radar"));
    scanBtn.setText(ts.get("button.scan"));
    exitBtn.setText(ts.get("button.exit"));

    // Update ship list item texts
    shipSpanMap.forEach((shipData, span) ->
        span.setText(buildShipInfoText(shipData)));
  }

  private String buildShipInfoText(ShipData shipData) {
    return shipData.getShipName()
        + " " + ts.get("ship.info.sector") + ": "
        + "(" + shipData.getSectorX() + "," + shipData.getSectorY() + ")"
        + " " + ts.get("ship.info.direction") + ": "
        + Directions.fromDelta(shipData.getDirectionX(), shipData.getDirectionY());
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
          c.getElement().getStyle().setBackground("#f0f4f8"));;
      itemLayout.getElement().getStyle().setBackground("#c3e0ff");
      selectedShipData = shipData;
      navigation.setupShipOnControlPanel(Directions.fromDelta(selectedShipData.getDirectionX(), selectedShipData.getDirectionY()));
    });
    shipList.add(itemLayout);
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

    shipList.setHeight("200px");
    shipList.setMaxHeight("200px");
    shipList.getStyle()
        .set("overflow-y", "auto")
        .set("overflow-x", "hidden")
        .set("border", "1px solid #e0e0e0")
        .set("border-radius", "6px")
        .setBackground("#ffffff");

    shipList.setMinHeight("120px");

    allShipData = shipService.getShips();

    for (ShipData shipData : allShipData) {
      addShipToControlPanel(shipData);
      sea.placeShipOnSea(shipData);
    }
  }
  private void refreshShipList(){
    shipList.removeAll();
    shipList.setPadding(false);
    shipList.setSpacing(false);

    shipList.setHeight("200px");
    shipList.setMaxHeight("200px");
    shipList.getStyle()
        .set("overflow-y", "auto")
        .set("overflow-x", "hidden")
        .set("border", "1px solid #e0e0e0")
        .set("border-radius", "6px")
        .setBackground("#ffffff");

    shipList.setMinHeight("120px");

    allShipData = shipService.getShips();

    for (ShipData shipData : allShipData) {
      addShipToControlPanel(shipData);
    }
  }
}
