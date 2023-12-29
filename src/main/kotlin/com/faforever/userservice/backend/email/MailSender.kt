package com.faforever.userservice.backend.email

import io.quarkus.mailer.Mail
import io.quarkus.mailer.Mailer
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class MailSender(
    private val mailer: Mailer,
) {
    fun sendMail(toEmail: String, subject: String, content: String, contentType: ContentType) {
        mailer.send(
            when (contentType) {
                ContentType.TEXT -> Mail.withText(toEmail, subject, content)
                ContentType.HTML -> Mail.withHtml(toEmail, subject, content)
            },
        )
    }
}

enum class ContentType {
    TEXT, HTML
}
