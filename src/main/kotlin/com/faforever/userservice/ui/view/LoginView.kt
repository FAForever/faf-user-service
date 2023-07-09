package com.example.starter.base.ui.view

import com.example.starter.base.ui.component.LoginCard
import com.example.starter.base.ui.layout.MainLayout
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("/oauth2/login", layout = MainLayout::class)
class LoginView(loginCard: LoginCard) : VerticalLayout() {

    init {
        add(loginCard)

        alignItems = FlexComponent.Alignment.CENTER
    }

}