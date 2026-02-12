package com.restaurantpos.identityaccess.repository;

import com.restaurantpos.identityaccess.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Finds a refresh token by its hash.
     * 
     * @param tokenHash the token hash
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Revokes all refresh tokens for a user.
     * 
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);
    
    /**
     * Deletes all expired refresh tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
