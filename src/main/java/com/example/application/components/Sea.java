package com.example.application.components;

import com.example.application.entity.*;
import com.example.application.service.ShipCommandService;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SpringComponent
@UIScope
public class Sea extends Div {

  // Unknown/not-yet-loaded sectors use a neutral color (not water-like blue).
  private static final String INITIAL_COLOR = "#4b5563";
  private static final double[] ZOOM_LEVELS = {1, 2, 3, 5, 8};
  private static final int BASE_SIZE = 720;

  private final ShipCommandService shipService;
  public final Div[][] cells = new Div[100][100];
  private final Map<String, Div> shipCells = new HashMap<>();
  private final Div gridContainer = new Div();

  private int currentZoomIndex = 0;
  // Do not serialize runtime UI callback across restarts/hot-reload.
  private transient Consumer<Double> wheelZoomListener;

  public Sea(ShipCommandService shipService) {
    this.shipService = shipService;
    setSeaContainerLayout();
  }

  @PostConstruct
  public void init(){
    createGrid();
    loadMap();
    attachWheelZoomJs();
  }

  private void createGrid() {
    for (int y = 0; y < 100; y++) {
      for (int x = 0; x < 100; x++) {
        Div cell = createBaseCell();
        cell.getStyle()
            .set("grid-column", String.valueOf(x + 1))
            .set("grid-row",    String.valueOf(100 - y));
        cells[x][y] = cell;
        gridContainer.add(cell);
      }
    }
  }

  private void loadMap() {
    List<SectorInfo> sectors = shipService.loadMap();
    applyMapSectors(sectors);
  }

  public void applyMapSectors(List<SectorInfo> sectors) {
    if (sectors == null || sectors.isEmpty()) {
      return;
    }
    for (SectorInfo sector : sectors) {
      int x = sector.getSectorX();
      int y = sector.getSectorY();

      if (x < 0 || x >= 100 || y < 0 || y >= 100) {
        continue;
      }
      Div cell = cells[x][y];
      applySectorToCell(cell, sector);
    }
  }

  public void applySectorToCell(Div cell,SectorInfo sector) {
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
    String shipId = shipData.getShipId();
    if (shipId == null || shipId.isBlank()) {
      return;
    }

    Image img = new Image("images/ship.png", shipData.getShipName());
    img.setWidth("100%");
    img.setHeight("100%");
    img.getStyle()
        .set("object-fit", "contain");

    String shortName = getRotationShortName(shipData);

    String rotation = getRotateSetting(shortName);

    img.getStyle().set("transform", rotation);

    Div previousCell = shipCells.get(shipId);
    if (previousCell != null) {
      previousCell.getChildren()
          .filter(component -> component instanceof Image)
          .findFirst()
          .ifPresent(previousCell::remove);
    }

    Div cell = cells[x][y];
    cell.add(img);

    shipCells.put(shipId, cell);
  }

  public void removeShipFromSea(ShipData shipData) {
    String shipId = shipData.getShipId();
    if (shipId == null || shipId.isBlank()) {
      return;
    }
    Div cell = shipCells.remove(shipId);
    if (cell != null) {
      cell.getChildren()
          .filter(component -> component instanceof Image)
          .findFirst()
          .ifPresent(cell::remove);
    }
  }

  public void moveShip(ShipData shipData, Directions direction,Directions directionAfterNavigate) {
    int oldX = shipData.getSectorX();
    int oldY = shipData.getSectorY();

    System.out.println("sector :" + oldX + " ..... " + oldY);

    int newX = oldX + direction.getDx();
    int newY = oldY + direction.getDy();
    System.out.println("sector :" + newX + " ..... " + newY);

    String shipId = shipData.getShipId();
    if (shipId == null || shipId.isBlank()) {
      return;
    }

    Div oldCell = shipCells.get(shipId);
    if (oldCell != null) {
      oldCell.getChildren()
          .filter(component -> component instanceof Image)
          .findFirst()
          .ifPresent(oldCell::remove);
    }

    shipData.setSectorX(newX);
    shipData.setSectorY(newY);
    shipData.setDirectionX(directionAfterNavigate.getDx());
    shipData.setDirectionY(directionAfterNavigate.getDy());
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

  public void setZoom(double zoomLevel) {
    // Find the matching zoom index
    for (int i = 0; i < ZOOM_LEVELS.length; i++) {
      if (ZOOM_LEVELS[i] == zoomLevel) {
        currentZoomIndex = i;
        break;
      }
    }
    int size = (int) (BASE_SIZE * zoomLevel);
    gridContainer.getStyle()
        .setWidth(size + "px")
        .setHeight(size + "px");
  }

  public void setWheelZoomListener(Consumer<Double> listener) {
    this.wheelZoomListener = listener;
  }

  @ClientCallable
  private void onWheelZoom(int zoomIndex) {
    if (zoomIndex >= 0 && zoomIndex < ZOOM_LEVELS.length) {
      currentZoomIndex = zoomIndex;
      if (wheelZoomListener != null) {
        wheelZoomListener.accept(ZOOM_LEVELS[currentZoomIndex]);
      }
    }
  }

  private void attachWheelZoomJs() {
    getElement().executeJs(
        "const wrapper = this;" +
        "const grid = this.firstElementChild;" +
        "const levels = [1, 2, 3, 5, 8];" +
        "let idx = 0;" +
        "wrapper.addEventListener('wheel', function(e) {" +
        "  if (!e.ctrlKey && !e.metaKey) return;" +
        "  e.preventDefault();" +
        // Determine new zoom index
        "  let newIdx = idx;" +
        "  if (e.deltaY < 0 && idx < levels.length - 1) newIdx = idx + 1;" +
        "  else if (e.deltaY > 0 && idx > 0) newIdx = idx - 1;" +
        "  else return;" +
        // Mouse position relative to the wrapper
        "  const rect = wrapper.getBoundingClientRect();" +
        "  const mouseX = e.clientX - rect.left;" +
        "  const mouseY = e.clientY - rect.top;" +
        // Content point under the mouse before zoom
        "  const contentX = wrapper.scrollLeft + mouseX;" +
        "  const contentY = wrapper.scrollTop + mouseY;" +
        // Scale factor between old and new zoom
        "  const scale = levels[newIdx] / levels[idx];" +
        // Apply new size
        "  const newSize = 720 * levels[newIdx];" +
        "  grid.style.width = newSize + 'px';" +
        "  grid.style.height = newSize + 'px';" +
        // Adjust scroll so the point under the mouse stays put
        "  wrapper.scrollLeft = contentX * scale - mouseX;" +
        "  wrapper.scrollTop = contentY * scale - mouseY;" +
        "  idx = newIdx;" +
        "  wrapper.$server.onWheelZoom(idx);" +
        "}, {passive: false});"
    );
  }

  private void setSeaContainerLayout() {
    // Outer wrapper: fixed size, scrollable
    getStyle()
        .setWidth(BASE_SIZE + "px")
        .setHeight(BASE_SIZE + "px")
        .setMaxWidth("95vw")
        .setOverflow(Style.Overflow.AUTO)
        .setBoxShadow("0 15px 40px rgba(0,0,0,0.65)")
        .setBackground("transparent")
        .setPosition(Style.Position.RELATIVE);

    // Inner grid container: zoomable
    gridContainer.getStyle()
        .setDisplay(Style.Display.GRID)
        .set("grid-template-columns", "repeat(100, 1fr)")
        .set("gap", "0")
        .setWidth(BASE_SIZE + "px")
        .setHeight(BASE_SIZE + "px")
        .setBackground("transparent");

    add(gridContainer);
  }

  private Div createBaseCell() {
    Div cell = new Div();
    cell.getStyle()
        // Border/Highlight soll die Zellgröße nicht verändern (verhindert "Zoom"-Effekt).
        .set("box-sizing", "border-box")
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

  public void applyAutoPilotStep(ShipData shipData, AutoPilotData data) {
    if (data.getSectorDataList() != null) {
      for (SectorData sd : data.getSectorDataList()) {
        int sx = sd.getSectorX();
        int sy = sd.getSectorY();
        if (sx < 0 || sx >= 100 || sy < 0 || sy >= 100) continue;

        Div cell = cells[sx][sy];
        SectorInfo info = new SectorInfo();
        info.setGround(sd.getGround());
        info.setDepth(sd.getDepth());
        info.setSectorX(sx);
        info.setSectorY(sy);
        applySectorToCell(cell, info);
      }
    }

    if (data.getShipPosition() != null) {
      int newX = data.getShipPosition().getX();
      int newY = data.getShipPosition().getY();

      String shipId = shipData.getShipId();
      if (shipId == null || shipId.isBlank()) {
        return;
      }

      Div oldCell = shipCells.get(shipId);
      if (oldCell != null) {
        oldCell.getChildren()
            .filter(c -> c instanceof Image)
            .findFirst()
            .ifPresent(oldCell::remove);
      }

      shipData.setSectorX(newX);
      shipData.setSectorY(newY);
      placeShipOnSea(shipData);
    }
  }

  public Div getCell(int x,int y){
    if (y < 0 || y >= 100 || x < 0 || x >= 100) return null;
    return cells[x][y];
  }

}
