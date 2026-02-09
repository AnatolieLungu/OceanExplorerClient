package com.example.application.components;

import com.example.application.entity.Directions;
import com.example.application.entity.Ground;
import com.example.application.entity.SectorInfo;
import com.example.application.entity.ShipData;
import com.example.application.service.ShipCommandService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringComponent
@UIScope
public class Sea extends Div {

  private static final String INITIAL_COLOR = "#6694e4ff";
  private final ShipCommandService shipService;
  private final Div[][] cells = new Div[100][100];
  private final Map<ShipData, Div> shipCells = new HashMap<>();

  public Sea(ShipCommandService shipService) {
    this.shipService = shipService;
    setSeaContainerLayout();
  }

  @PostConstruct
  public void init(){
    createGrid();
    loadMap();
  }

  private void createGrid() {
    for (int y = 0; y < 100; y++) {
      for (int x = 0; x < 100; x++) {
        Div cell = createBaseCell();
        cell.getStyle()
            .set("grid-column", String.valueOf(x + 1))
            .set("grid-row",    String.valueOf(100 - y));
        cells[x][y] = cell;
        add(cell);
      }
    }
  }

  private void loadMap() {
    List<SectorInfo> sectors = shipService.loadMap();
    for (SectorInfo sector : sectors) {
      int x = sector.getSectorX();
      int y = sector.getSectorY();

      if (x < 0 || x >= 99 || y < 0 || y >= 99) {
        continue;
      }
      Div cell = cells[y][x];
      applySectorToCell(cell, sector);
    }
  }

  private void applySectorToCell(Div cell,SectorInfo sector) {
    String bgColor = getGroundColor(sector.getGround());
    if (sector.getGround() == Ground.Water && sector.getDepth() > 200) {
      bgColor = darkenColor(bgColor, 0.25);
    }
    cell.getStyle().setBackground(bgColor);
  }

  private String darkenColor(String hexColor, double factor) {
    if (hexColor == null || !hexColor.startsWith("#")) {
      return hexColor;
    }

    try {
      hexColor = hexColor.replace("#", "");
      int r = Integer.parseInt(hexColor.substring(0, 2), 16);
      int g = Integer.parseInt(hexColor.substring(2, 4), 16);
      int b = Integer.parseInt(hexColor.substring(4, 6), 16);

      r = (int) Math.max(0, r * (1 - factor));
      g = (int) Math.max(0, g * (1 - factor));
      b = (int) Math.max(0, b * (1 - factor));

      return String.format("#%02x%02x%02x", r, g, b);
    } catch (Exception e) {
      return hexColor;
    }
  }

  private String getGroundColor(Ground ground) {
    if (ground == null) {
      return "#2F4F4F";
    }
    return switch (ground) {
      case Water   -> "#6694e4ff";
      case Land    -> "#8B4513";
      case Ice     -> "#E0FFFF";
      case Harbour -> "#483D8B";
      case None    -> "#2F4F4F";
    };
  }

  public String getRotationShortName(ShipData shipData) {
    Directions direction = Directions.fromDelta(shipData.getDirectionX(),shipData.getDirectionY());
    return direction.getShortName();
  }

  public void placeShipOnSea(ShipData shipData) {
    int x = shipData.getSectorX();
    int y = shipData.getSectorY();

    Image img = new Image("images/ship.png", shipData.getName());
    img.setWidth("100%");
    img.setHeight("100%");
    img.getStyle()
        .set("object-fit", "contain");

    String shortName = getRotationShortName(shipData);

    String rotation = getRotateSetting(shortName);

    img.getStyle().set("transform", rotation);

    Div cell = cells[x][y];;
    cell.add(img);

    shipCells.put(shipData, cell);
  }

  public void moveShip(ShipData shipData, Directions direction) {
    int oldX = shipData.getSectorX();
    int oldY = shipData.getSectorY();

    int newX = oldX + direction.getDx();
    int newY = oldY + direction.getDy();

    Div oldCell = shipCells.get(shipData);
    if (oldCell != null) {
      oldCell.getChildren()
          .filter(component -> component instanceof Image)
          .findFirst()
          .ifPresent(oldCell::remove);
    }

    shipData.setSectorX(newX);
    shipData.setSectorY(newY);
    shipData.setDirectionX(direction.getDx());
    shipData.setDirectionY(direction.getDy());

    placeShipOnSea(shipData);
  }

  private String getRotateSetting(String direction) {
    return switch (direction) {
      case "N"  -> "rotate(0deg)";
      case "NE" -> "rotate(45deg)";
      case "E"  -> "rotate(90deg)";
      case "SE" -> "rotate(135deg)";
      case "S"  -> "rotate(180deg)";
      case "SW" -> "rotate(225deg)";
      case "W"  -> "rotate(270deg)";
      case "NW" -> "rotate(315deg)";
      default -> "rotate(0deg)";
    };
  }

  private void setSeaContainerLayout() {
    getStyle()
        .setDisplay(Style.Display.GRID)
        .set("grid-template-columns", "repeat(100, 1fr)")
        .set("gap", "0")
        .setWidth("720px")
        .setMaxWidth("95vw")
        .set("aspect-ratio", "1 / 1")
        .setOverflow(Style.Overflow.HIDDEN)
        .setBoxShadow("0 15px 40px rgba(0,0,0,0.65)")
        .setBackground("transparent");

  }

  private Div createBaseCell() {
    Div cell = new Div();
    cell.getStyle()
        .setWidth("100%")
        .setHeight("100%")
        .setBackground(INITIAL_COLOR)
        .setPosition(Style.Position.RELATIVE)
        .setOverflow(Style.Overflow.HIDDEN)
        .setDisplay(Style.Display.FLEX)
        .setAlignItems(Style.AlignItems.CENTER)
        .setJustifyContent(Style.JustifyContent.CENTER)
        .setFontSize("16px")
        .setColor("white");

    return cell;
  }

}
