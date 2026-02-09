package com.example.application.views;
import com.example.application.components.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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