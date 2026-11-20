package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.AltaRestriccionChat;
import java.util.Optional;

public interface AltaRestriccionChatRepository {
    Optional<AltaRestriccionChat> findById(String claveHash);
    AltaRestriccionChat save(AltaRestriccionChat alta);
}
