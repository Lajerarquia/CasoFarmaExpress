package cl.duoc.farmaexpress.bff.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GatewayExceptionHandler {
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProblemDetail> unavailable(ResourceAccessException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "No fue posible comunicarse con el servicio interno");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }
}
