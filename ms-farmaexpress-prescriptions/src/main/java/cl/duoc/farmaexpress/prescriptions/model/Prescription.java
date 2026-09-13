package cl.duoc.farmaexpress.prescriptions.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRESCRIPTIONS")
public class Prescription {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "prescription_seq"
    )
    @SequenceGenerator(
            name = "prescription_seq",
            sequenceName = "PRESCRIPTION_SEQ",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "PATIENT_ID", nullable = false, length = 100)
    private String patientId;

    @Column(name = "PHARMACY_ID", nullable = false, length = 100)
    private String pharmacyId;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private PrescriptionStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Prescription() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();

        if (status == null) {
            status = PrescriptionStatus.INGRESADA;
        }

        createdAt = ahora;
        updatedAt = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}