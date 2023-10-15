package com.faforever.userservice.backend.cloudflare

import com.faforever.userservice.backend.security.HmacService
import jakarta.enterprise.context.ApplicationScoped
import java.net.URI

@ApplicationScoped
class CloudflareService(
    private val hmacService: HmacService,
) {

    /**
     * Builds hmac token for cloudflare firewall verification as specified
     * [here](https://support.cloudflare.com/hc/en-us/articles/115001376488-Configuring-Token-Authentication)
     * @param uri uri to generate the hmac token for
     * @return string representing the hmac token formatted as {timestamp}-{hashedContent}
     */
    fun generateCloudFlareHmacToken(uri: URI, secret: String): String {
        val message = if (uri.path.startsWith("/")) uri.path else "/" + uri.path
        return hmacService.generateHmacToken(message, secret)
    }
}
