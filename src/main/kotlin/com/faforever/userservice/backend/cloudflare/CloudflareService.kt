package com.faforever.userservice.backend.cloudflare

import jakarta.enterprise.context.ApplicationScoped
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@ApplicationScoped
class CloudflareService {

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
    }

    /**
     * Builds hmac token for cloudflare firewall verification as specified
     * [here](https://support.cloudflare.com/hc/en-us/articles/115001376488-Configuring-Token-Authentication)
     * @param uri uri to generate the hmac token for
     * @return string representing the hmac token formatted as {timestamp}-{hashedContent}
     */
    fun generateCloudFlareHmacToken(uri: String, secret: String): String {
        return generateCloudFlareHmacToken(URI.create(uri), secret)
    }

    /**
     * Builds hmac token for cloudflare firewall verification as specified
     * [here](https://support.cloudflare.com/hc/en-us/articles/115001376488-Configuring-Token-Authentication)
     * @param uri uri to generate the hmac token for
     * @return string representing the hmac token formatted as {timestamp}-{hashedContent}
     */
    fun generateCloudFlareHmacToken(uri: URI, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256))

        val timeStamp = Instant.now().epochSecond
        val macMessage = (uri.getPath() + timeStamp).toByteArray(StandardCharsets.UTF_8)
        val hmacEncoded = URLEncoder.encode(
            String(Base64.getEncoder().encode(mac.doFinal(macMessage)), StandardCharsets.UTF_8),
            StandardCharsets.UTF_8
        )
        return "${timeStamp}-${hmacEncoded}"
    }

}