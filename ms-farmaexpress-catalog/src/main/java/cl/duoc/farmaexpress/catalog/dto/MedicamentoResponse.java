package cl.duoc.farmaexpress.catalog.dto;

import cl.duoc.farmaexpress.catalog.model.Medicamento;
import java.math.BigDecimal;

public record MedicamentoResponse(Long id, String sku, String nombre, BigDecimal precio, Integer stock) {
    public static MedicamentoResponse from(Medicamento medicamento) {
        return new MedicamentoResponse(medicamento.getId(), medicamento.getSku(), medicamento.getNombre(),
                medicamento.getPrecio(), medicamento.getStock());
    }
}
