package com.uphead.inventory.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // @Modifying queries need an explicit transaction (unlike save()/findBy...,
    // they don't get one implicitly). REQUIRES_NEW so this always commits on
    // its own, independent of any caller transaction — RefreshTokenService's
    // reuse-detection relies on that: it revokes, then throws, and this must
    // not roll back with the exception.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    void revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
