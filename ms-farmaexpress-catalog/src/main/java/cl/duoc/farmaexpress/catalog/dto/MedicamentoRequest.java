package cl.duoc.farmaexpress.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MedicamentoRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @NotNull @PositiveOrZero Integer stock
) {}
