package com.vortise.gestion.infrastructure.persistence;
import com.vortise.gestion.domain.model.InvitacionEquipo;
import com.vortise.gestion.domain.repository.InvitacionEquipoRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface JpaInvitacionEquipoRepository extends JpaRepository<InvitacionEquipo, Long>, InvitacionEquipoRepository {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitacionEquipo i where i.id = :id")
    Optional<InvitacionEquipo> bloquearPorId(@Param("id") Long id);
}
