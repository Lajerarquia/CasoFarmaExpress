package cl.duoc.farmaexpress.prescriptions.service;

import cl.duoc.farmaexpress.prescriptions.dto.CreatePrescriptionRequest;
import cl.duoc.farmaexpress.prescriptions.dto.UpdateStatusRequest;
import cl.duoc.farmaexpress.prescriptions.model.Prescription;
import cl.duoc.farmaexpress.prescriptions.model.PrescriptionStatus;
import cl.duoc.farmaexpress.prescriptions.repository.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public Prescription create(CreatePrescriptionRequest request) {

        Prescription prescription = new Prescription();

        prescription.setPatientId(request.getPatientId());
        prescription.setPharmacyId(request.getPharmacyId());
        prescription.setImageUrl(request.getImageUrl());
        prescription.setStatus(PrescriptionStatus.INGRESADA);

        return prescriptionRepository.save(prescription);
    }

    @Transactional(readOnly = true)
    public Prescription getById(Long id) {

        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receta no encontrada con id: " + id
                ));
    }

    public Prescription updateStatus(
            Long id,
            UpdateStatusRequest request
    ) {

        Prescription prescription = getById(id);

        PrescriptionStatus currentStatus = prescription.getStatus();
        PrescriptionStatus newStatus = request.getStatus();

        validateTransition(currentStatus, newStatus);

        prescription.setStatus(newStatus);

        return prescriptionRepository.save(prescription);
    }

    @Transactional(readOnly = true)
    public List<Prescription> find(
            PrescriptionStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {

        if (status != null && from != null && to != null) {
            return prescriptionRepository
                    .findByStatusAndCreatedAtBetween(status, from, to);
        }

        if (status != null) {
            return prescriptionRepository.findByStatus(status);
        }

        if (from != null && to != null) {
            return prescriptionRepository.findByCreatedAtBetween(from, to);
        }

        if ((from == null && to != null)
                || (from != null && to == null)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar from y to juntos"
            );
        }

        return prescriptionRepository.findAll();
    }

    private void validateTransition(
            PrescriptionStatus current,
            PrescriptionStatus next
    ) {

        if (current == next) {
            throw invalidTransition(current, next);
        }

        boolean valid = switch (current) {

            case INGRESADA ->
                    next == PrescriptionStatus.VALIDADA
                    || next == PrescriptionStatus.RECHAZADA;

            case VALIDADA ->
                    next == PrescriptionStatus.EN_PREPARACION
                    || next == PrescriptionStatus.RECHAZADA;

            case EN_PREPARACION ->
                    next == PrescriptionStatus.LISTA_RETIRO;

            case LISTA_RETIRO ->
                    next == PrescriptionStatus.DISPENSADA;

            case DISPENSADA, RECHAZADA -> false;
        };

        if (!valid) {
            throw invalidTransition(current, next);
        }
    }

    private ResponseStatusException invalidTransition(
            PrescriptionStatus current,
            PrescriptionStatus next
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transición de estado no permitida: "
                        + current.getValorApi()
                        + " -> "
                        + next.getValorApi()
        );
    }
}