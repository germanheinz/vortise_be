package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.PasswordResetToken;
import com.vortise.gestion.repository.PasswordResetTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>, PasswordResetTokenRepository { }
