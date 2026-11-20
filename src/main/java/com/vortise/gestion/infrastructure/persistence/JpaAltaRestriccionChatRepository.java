package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.AltaRestriccionChat;
import com.vortise.gestion.domain.repository.AltaRestriccionChatRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAltaRestriccionChatRepository
        extends JpaRepository<AltaRestriccionChat, String>, AltaRestriccionChatRepository {
}
