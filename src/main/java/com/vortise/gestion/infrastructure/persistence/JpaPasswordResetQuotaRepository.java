package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.model.PasswordResetQuota;
import com.vortise.gestion.repository.PasswordResetQuotaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPasswordResetQuotaRepository extends JpaRepository<PasswordResetQuota, String>, PasswordResetQuotaRepository { }
