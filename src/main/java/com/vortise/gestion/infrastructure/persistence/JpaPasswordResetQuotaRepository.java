package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.PasswordResetQuota;
import com.vortise.gestion.domain.repository.PasswordResetQuotaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPasswordResetQuotaRepository extends JpaRepository<PasswordResetQuota, String>, PasswordResetQuotaRepository { }
