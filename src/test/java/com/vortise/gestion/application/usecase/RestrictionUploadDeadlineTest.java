package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Proyecto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestrictionUploadDeadlineTest {
    @Test
    void includesTheDeadlineDay() {
        Proyecto project = project(LocalDate.of(2026, 11, 1), 5);
        LocalDate deadline = LocalDate.of(2026, 11, 6);

        assertEquals(deadline, RestrictionUploadDeadline.deadline(project));
        RestrictionUploadDeadline.validate(project, deadline);
    }

    @Test
    void rejectsNewRestrictionsAfterTheDeadline() {
        Proyecto project = project(LocalDate.of(2026, 11, 1), 0);

        assertThrows(SolicitudInvalidaException.class,
            () -> RestrictionUploadDeadline.validate(project, LocalDate.of(2026, 11, 2)));
    }

    @Test
    void leavesProjectsWithoutAConfiguredStartOrDeadlineUnlimited() {
        assertNull(RestrictionUploadDeadline.deadline(project(null, 5)));
        assertNull(RestrictionUploadDeadline.deadline(project(LocalDate.of(2026, 11, 1), null)));
    }

    private Proyecto project(LocalDate start, Integer days) {
        Proyecto project = new Proyecto();
        project.setFechaInicio(start);
        project.setPedidosDeObraDias(days);
        return project;
    }
}