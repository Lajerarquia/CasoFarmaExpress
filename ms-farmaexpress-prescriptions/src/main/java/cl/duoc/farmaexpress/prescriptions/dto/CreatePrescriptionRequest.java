package cl.duoc.farmaexpress.prescriptions.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePrescriptionRequest {

    @NotBlank
    private String patientId;

    @NotBlank
    private String pharmacyId;

    private String imageUrl;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}