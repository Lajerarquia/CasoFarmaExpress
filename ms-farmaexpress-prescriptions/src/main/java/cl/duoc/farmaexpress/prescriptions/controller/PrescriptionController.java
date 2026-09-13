package cl.duoc.farmaexpress.prescriptions.controller;

import cl.duoc.farmaexpress.prescriptions.dto.CreatePrescriptionRequest;
import cl.duoc.farmaexpress.prescriptions.dto.UpdateStatusRequest;
import cl.duoc.farmaexpress.prescriptions.model.Prescription;
import cl.duoc.farmaexpress.prescriptions.model.PrescriptionStatus;
import cl.duoc.farmaexpress.prescriptions.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public ResponseEntity<Prescription> create(
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        Prescription prescription = prescriptionService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(prescription);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                prescriptionService.getById(id)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Prescription> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(
                prescriptionService.updateStatus(id, request)
        );
    }

    @GetMapping
    public ResponseEntity<List<Prescription>> find(
            @RequestParam(required = false)
            PrescriptionStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        return ResponseEntity.ok(
                prescriptionService.find(status, from, to)
        );
    }
}