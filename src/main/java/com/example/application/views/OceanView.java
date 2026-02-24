package com.example.application.views;

import com.example.application.components.*;
import com.example.application.i18n.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

@Route("")
@SpringComponent
@UIScope
public class OceanView extends VerticalLayout {

    public OceanView(ControlPanel controlPanel, Sea sea, TranslationService translationService) {
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().setBackground("#0a192f");

        // ── Header: title + language toggle ──────────────────────────────
        Div header = new Div();
        header.getStyle()
            .set("position", "relative")
            .set("width", "100%")
            .set("text-align", "center");

        header.add(AppTitle.create());

        // Language toggle button (top-right corner)
        Span langLabel = new Span(translationService.getCurrentLanguage().toUpperCase());
        langLabel.getStyle()
            .setColor("white")
            .setFontSize("14px")
            .set("font-weight", "600");

        Button langToggle = new Button();
        langToggle.getElement().appendChild(langLabel.getElement());
        langToggle.getStyle()
            .setPosition(Style.Position.ABSOLUTE)
            .set("right", "24px")
            .set("top", "50%")
            .set("transform", "translateY(-50%)")
            .setBackground("rgba(102, 153, 255, 0.25)")
            .setColor("white")
            .setBorder("1px solid rgba(102, 153, 255, 0.5)")
            .setBorderRadius("8px")
            .setPadding("6px 14px")
            .setCursor("pointer")
            .setFontSize("14px")
            .set("font-weight", "600")
            .set("backdrop-filter", "blur(4px)")
            .set("-webkit-backdrop-filter", "blur(4px)")
            .set("transition", "background 0.2s");

        langToggle.addClickListener(e -> {
            translationService.toggleLanguage();
            langLabel.setText(translationService.getCurrentLanguage().toUpperCase());
        });

        header.add(langToggle);
        add(header);

        // ── Body ─────────────────────────────────────────────────────────
        ZoomControl zoomControl = new ZoomControl();

        // Wire zoom: button clicks -> sea zoom
        zoomControl.setZoomChangeListener(sea::setZoom);
        // Wire zoom: mouse wheel -> update zoom control label
        sea.setWheelZoomListener(zoomControl::setZoomLevel);

        // Wrapper with position:relative so ZoomControl overlays on the Sea
        Div seaWrapper = new Div();
        seaWrapper.getStyle()
            .setPosition(Style.Position.RELATIVE)
            .setDisplay(Style.Display.INLINE_BLOCK);

        // Position ZoomControl as an overlay on top-right of the sea
        zoomControl.getStyle()
            .setPosition(Style.Position.ABSOLUTE)
            .set("top", "12px")
            .set("right", "12px")
            .set("z-index", "10");

        seaWrapper.add(sea, zoomControl);

        MainLayout mainLayout = new MainLayout();
        mainLayout.add(controlPanel.getShipSelectionPanel(), seaWrapper, controlPanel);

        add(mainLayout);

        // ── Footer ───────────────────────────────────────────────────────
        Div spacer = new Div();
        spacer.setHeight("30px");
        add(spacer);
    }
}
