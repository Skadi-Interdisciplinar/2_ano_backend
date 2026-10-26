package skadi.api.config;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import skadi.api.dto.ErrorResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.AlertHandlingNotFoundException;
import skadi.api.exceptions.AlertNotFoundException;
import skadi.api.exceptions.ReportNotFoundException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.exceptions.JustificationAlreadyExistsException;
import skadi.api.exceptions.JustificationNotFoundException;
import skadi.api.exceptions.JustificationRuleViolationException;
import skadi.api.exceptions.SignatureAlreadyExistsException;
import skadi.api.exceptions.SignatureNotFoundException;
import skadi.api.exceptions.CategoryAlreadyExistsException;
import skadi.api.exceptions.CategoryInvalidTemperatureRangeException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.exceptions.BatchAlreadyExistsException;
import skadi.api.exceptions.BatchInvalidDatesException;
import skadi.api.exceptions.BatchNotFoundException;
import skadi.api.exceptions.BatchRuleViolationException;
import skadi.api.exceptions.BatchStorageNotFoundException;
import skadi.api.exceptions.BatchStorageRuleViolationException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.TemperatureReadingNotFoundException;
import skadi.api.exceptions.CurrentTemperatureNotFoundException;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.exceptions.AddressAlreadyExistsException;
import skadi.api.exceptions.AddressNotFoundException;
import skadi.api.exceptions.StateNotFoundException;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.exceptions.ThermometerRuleViolationException;
import skadi.api.exceptions.ColdRoomInvalidTemperatureRangeException;
import skadi.api.exceptions.ColdRoomRuleViolationException;
import skadi.api.exceptions.NotificationNotFoundException;
import skadi.api.exceptions.ResourceOutsideScopeException;

import java.sql.Timestamp;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({
        ReportNotFoundException.class,
        SignatureNotFoundException.class,
        CategoryNotFoundException.class,
        BatchNotFoundException.class, 
        BatchStorageNotFoundException.class,
        TemperatureReadingNotFoundException.class, 
        CurrentTemperatureNotFoundException.class,
        ColdRoomNotFoundException.class,
        JustificationNotFoundException.class,
        AlertNotFoundException.class,
        AlertHandlingNotFoundException.class,
        AddressNotFoundException.class,
        StateNotFoundException.class,
        ThermometerNotFoundException.class,
        NotificationNotFoundException.class,
        ResourceOutsideScopeException.class})
    public ResponseEntity<ErrorResponse> handleResourcesNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler({ReportRuleViolationException.class, BatchRuleViolationException.class,
        BatchStorageRuleViolationException.class,
        JustificationRuleViolationException.class, JustificationAlreadyExistsException.class,
        ThermometerRuleViolationException.class, ColdRoomRuleViolationException.class})
    public ResponseEntity<ErrorResponse> handleRuleViolation(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "O registro não pode ser excluído ou alterado porque possui dados relacionados",
                        Timestamp.from(Instant.now())
                ));
    }

    @ExceptionHandler({
            CategoryAlreadyExistsException.class,
            BatchAlreadyExistsException.class,
            SignatureAlreadyExistsException.class,
            UserAlreadyExistsException.class,
            AddressAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleAlreadyExists(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(BatchInvalidDatesException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBatchDates(BatchInvalidDatesException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler({ColdRoomInvalidTemperatureRangeException.class,
            CategoryInvalidTemperatureRangeException.class})
    public ResponseEntity<ErrorResponse> handleInvalidColdRoomTemperatureRange(
            ColdRoomInvalidTemperatureRangeException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage(),Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(),Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(),Timestamp.from(Instant.now())) );
    }

    @ExceptionHandler(CdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCdNotFound(CdNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(),Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Dados inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, Timestamp.from(Instant.now())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "O corpo da requisição possui um valor ou formato inválido",
                        Timestamp.from(Instant.now())
                ));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredToken(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(),Timestamp.from(Instant.now())));
    }
}
