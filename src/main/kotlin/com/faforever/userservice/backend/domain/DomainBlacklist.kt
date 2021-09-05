package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity(name = "email_domain_blacklist")
data class DomainBlacklist(
    @Id
    val domain: String
)

@ApplicationScoped
class DomainBlacklistRepository : PanacheRepositoryBase<DomainBlacklist, String> {
    fun existsByDomain(domain: String): Boolean = count("domain = ?1", domain) > 0
}
