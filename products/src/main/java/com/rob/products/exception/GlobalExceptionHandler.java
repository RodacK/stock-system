package com.rob.products.exception;

import com.rob.products.jsonapi.JsonApiError;
import com.rob.products.jsonapi.JsonApiErrorDocument;
import com.rob.products.jsonapi.JsonApiErrorSource;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType JSON_API = MediaType.valueOf("application/vnd.api+json");

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<JsonApiErrorDocument> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(JSON_API)
                .body(new JsonApiErrorDocument(List.of(
                        JsonApiError.builder()
                                .status("404")
                                .title("Product Not Found")
                                .detail(ex.getMessage())
                                .build()
                )));
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<JsonApiErrorDocument> handleProductAlreadyExists(ProductAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(JSON_API)
                .body(new JsonApiErrorDocument(List.of(
                        JsonApiError.builder()
                                .status("409")
                                .title("Product Already Exists")
                                .detail(ex.getMessage())
                                .build()
                )));
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<JsonApiErrorDocument> handleRateLimit(RequestNotPermitted ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(JSON_API)
                .body(new JsonApiErrorDocument(List.of(
                        JsonApiError.builder()
                                .status("429")
                                .title("Too Many Requests")
                                .detail("Rate limit exceeded. Please try again later.")
                                .build()
                )));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonApiErrorDocument> handleValidation(MethodArgumentNotValidException ex) {
        List<JsonApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> JsonApiError.builder()
                        .status("400")
                        .title("Validation Error")
                        .detail(fe.getDefaultMessage())
                        .source(JsonApiErrorSource.builder()
                                .pointer("/data/attributes/" + fe.getField())
                                .build())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .contentType(JSON_API)
                .body(new JsonApiErrorDocument(errors));
    }
}