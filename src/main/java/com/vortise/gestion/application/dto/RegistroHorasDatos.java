package com.vortise.gestion.application.dto;

/** Shared input contract for the same business rules on creation and editing. */
public interface RegistroHorasDatos {
    String numeroRubro(); String rubro(); String subRubro(); String responsable(); String etapa();
    java.time.OffsetDateTime inicio(); java.time.OffsetDateTime fin(); String descripcion();
    Double cantidadPrevista(); Double cantidadReal(); Double horasPrevistas();
    Double horasOficiales(); Double horasAyudantes(); String causaNoCumplimiento(); String medidaCorrectiva();
}
