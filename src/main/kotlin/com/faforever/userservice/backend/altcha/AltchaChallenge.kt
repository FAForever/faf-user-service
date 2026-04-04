package com.faforever.userservice.backend.altcha

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import java.time.LocalDateTime

@Entity(name = "altcha_challenge")
data class AltchaChallenge(
    @Id
    val challenge: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
)

@ApplicationScoped
class AltchaChallengeRepository : PanacheRepositoryBase<AltchaChallenge, String> {

    /**
     * Atomically consumes the challenge: deletes it if it exists and has not expired.
     * Returns true if the challenge was found and deleted (i.e. valid and first use).
     */
    fun consumeChallenge(challenge: String): Boolean =
        delete("challenge = ?1 and expiresAt > ?2", challenge, LocalDateTime.now()) > 0

    @Transactional
    @Scheduled(every = "10m")
    fun deleteExpired() {
        delete("expiresAt <= ?1", LocalDateTime.now())
    }
}
