package flipnote.user.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import flipnote.user.domain.entity.OAuthLink;

public interface OAuthLinkRepository extends JpaRepository<OAuthLink, Long> {

    @Query("""
            SELECT ol FROM OAuthLink ol
            JOIN FETCH ol.user
            WHERE ol.provider = :provider AND ol.providerId = :providerId
            """)
    Optional<OAuthLink> findByProviderAndProviderIdWithUser(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

    boolean existsByProviderAndProviderId(String provider, String providerId);

    boolean existsByIdAndUser_Id(Long id, Long userId);

    List<OAuthLink> findByUser_Id(Long userId);
}
