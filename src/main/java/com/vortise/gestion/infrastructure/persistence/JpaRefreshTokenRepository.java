package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.RefreshToken;
import com.vortise.gestion.repository.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository { }
