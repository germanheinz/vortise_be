package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.VinculoTelegram;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVinculoTelegramRepository extends JpaRepository<VinculoTelegram, Long> {
    @org.springframework.data.jpa.repository.Query("select v.usuarioId from VinculoTelegram v where v.codigoHash = :hash")
    Optional<Long> buscarUsuarioPorCodigo(@org.springframework.data.repository.query.Param("hash") String hash);
    Optional<VinculoTelegram> findByCodigoHash(String hash);
    Optional<VinculoTelegram> findByChatConfirmado(Long chatId);
}
