package com.vortise.gestion.domain.repository;
import com.vortise.gestion.domain.model.InvitacionEquipo;
import java.util.*;
public interface InvitacionEquipoRepository {
    InvitacionEquipo save(InvitacionEquipo invitacion);
    Optional<InvitacionEquipo> findByTokenHash(String hash);
    interface Identidad { Long getId(); Long getTitularId(); }
    @org.springframework.data.jpa.repository.Query("select i.id as id, i.titularId as titularId from InvitacionEquipo i where i.tokenHash = :hash")
    Optional<Identidad> buscarIdentidad(@org.springframework.data.repository.query.Param("hash") String hash);
    Optional<InvitacionEquipo> findById(Long id);
    Optional<InvitacionEquipo> bloquearPorId(Long id);
    List<InvitacionEquipo> findByTitularIdOrderByIdDesc(Long titularId);
}
