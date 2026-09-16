package cl.duoc.farmaexpress.catalog.service;

import cl.duoc.farmaexpress.catalog.dto.MedicamentoRequest;
import cl.duoc.farmaexpress.catalog.dto.MedicamentoResponse;
import cl.duoc.farmaexpress.catalog.model.Medicamento;
import cl.duoc.farmaexpress.catalog.repository.MedicamentoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional
public class MedicamentoService {
    private final MedicamentoRepository repository;

    public MedicamentoService(MedicamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MedicamentoResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(MedicamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse getById(Long id) {
        return MedicamentoResponse.from(require(id));
    }

    public MedicamentoResponse create(MedicamentoRequest request) {
        if (repository.existsBySku(request.sku().trim())) {
            throw duplicateSku();
        }
        Medicamento medicamento = new Medicamento();
        apply(medicamento, request);
        return save(medicamento);
    }

    public MedicamentoResponse update(Long id, MedicamentoRequest request) {
        Medicamento medicamento = require(id);
        if (repository.existsBySkuAndIdNot(request.sku().trim(), id)) {
            throw duplicateSku();
        }
        apply(medicamento, request);
        return save(medicamento);
    }

    public void delete(Long id) {
        repository.delete(require(id));
    }

    private Medicamento require(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento no encontrado"));
    }

    private void apply(Medicamento medicamento, MedicamentoRequest request) {
        medicamento.setSku(request.sku().trim());
        medicamento.setNombre(request.nombre().trim());
        medicamento.setPrecio(request.precio());
        medicamento.setStock(request.stock());
    }

    private MedicamentoResponse save(Medicamento medicamento) {
        try {
            return MedicamentoResponse.from(repository.saveAndFlush(medicamento));
        } catch (DataIntegrityViolationException ex) {
            // The database constraint also handles concurrent creates/updates of the same SKU.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflicto al guardar el medicamento", ex);
        }
    }

    private ResponseStatusException duplicateSku() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un medicamento con ese SKU");
    }
}
