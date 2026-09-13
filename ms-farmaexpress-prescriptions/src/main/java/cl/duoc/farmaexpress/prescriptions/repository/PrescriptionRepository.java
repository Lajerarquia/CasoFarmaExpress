package cl.duoc.farmaexpress.prescriptions.repository;

import cl.duoc.farmaexpress.prescriptions.model.Prescription;
import cl.duoc.farmaexpress.prescriptions.model.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByStatus(PrescriptionStatus status);

    List<Prescription> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Prescription> findByStatusAndCreatedAtBetween(
            PrescriptionStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
}