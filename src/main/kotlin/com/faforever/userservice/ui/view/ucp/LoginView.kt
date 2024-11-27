package com.faforever.userservice.ui.view.ucp

import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import jakarta.servlet.http.HttpServletRequest


@Route("/ucp/login")
class LoginView(request: HttpServletRequest) : VerticalLayout() {

    init {
        val loginForm = LoginForm()


        loginForm.addLoginListener { e ->
            try {
                // Trigger form-based login using Quarkus' built-in mechanism
                request.login(e.username, e.password)
                loginForm.ui.ifPresent { ui -> ui.navigate("main") } // Navigate on successful login
            } catch (ex: Exception) {
                loginForm.isError = true // Show error on login failure
                Notification.show("Login failed. Check your credentials.")
            }
        }

        add(loginForm)
    }
}
