package cl.duoc.farmaexpress.prescriptions.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrescriptionStatus {

    INGRESADA("INGRESADA"),
    VALIDADA("VALIDADA"),
    EN_PREPARACION("EN_PREPARACIÓN"),
    LISTA_RETIRO("LISTA_RETIRO"),
    DISPENSADA("DISPENSADA"),
    RECHAZADA("RECHAZADA");

    private final String valorApi;

    PrescriptionStatus(String valorApi) {
        this.valorApi = valorApi;
    }

    @JsonValue
    public String getValorApi() {
        return valorApi;
    }

    @JsonCreator
    public static PrescriptionStatus fromValue(String value) {
        for (PrescriptionStatus status : values()) {
            if (status.valorApi.equalsIgnoreCase(value)
                    || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "Estado de receta no válido: " + value
        );
    }
}