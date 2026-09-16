package cl.duoc.farmaexpress.catalog.controller;

import cl.duoc.farmaexpress.catalog.dto.MedicamentoRequest;
import cl.duoc.farmaexpress.catalog.dto.MedicamentoResponse;
import cl.duoc.farmaexpress.catalog.service.MedicamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/medicamentos")
public class MedicamentoController {
    private final MedicamentoService service;

    public MedicamentoController(MedicamentoService service) { this.service = service; }

    @GetMapping
    public List<MedicamentoResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public MedicamentoResponse getById(@PathVariable Long id) { return service.getById(id); }

    @PostMapping
    public ResponseEntity<MedicamentoResponse> create(@Valid @RequestBody MedicamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public MedicamentoResponse update(@PathVariable Long id, @Valid @RequestBody MedicamentoRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
