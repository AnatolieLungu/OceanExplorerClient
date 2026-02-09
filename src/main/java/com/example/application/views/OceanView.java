package com.example.application.views;
import com.example.application.components.*;
import com.example.application.service.ShipCommandService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

@Route("")
@SpringComponent
@UIScope
public class OceanView extends VerticalLayout {

    public OceanView(ControlPanel controlPanel,Sea sea) {
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().setBackground("#0a192f");

        //Header
        add(AppTitle.create());

        //Body
        MainLayout mainLayout = new MainLayout();
        mainLayout.add(sea,controlPanel);

        add(mainLayout);


        //Footer
        Div spacer = new Div();
        spacer.setHeight("30px");
        add(spacer);
    }

}