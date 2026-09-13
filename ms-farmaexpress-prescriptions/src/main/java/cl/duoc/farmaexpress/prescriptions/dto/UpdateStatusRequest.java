package cl.duoc.farmaexpress.prescriptions.dto;

import cl.duoc.farmaexpress.prescriptions.model.PrescriptionStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull
    private PrescriptionStatus status;

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }
}