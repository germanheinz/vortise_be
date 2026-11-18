package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.InvitacionUsuario;
import com.vortise.gestion.domain.repository.InvitacionUsuarioRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInvitacionUsuarioRepository extends JpaRepository<InvitacionUsuario, Long>, InvitacionUsuarioRepository {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitacionUsuario i where i.id = :id")
    Optional<InvitacionUsuario> bloquearPorId(@Param("id") Long id);
}
