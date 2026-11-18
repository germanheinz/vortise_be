package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.RefreshToken;
import com.vortise.gestion.domain.repository.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository { }
