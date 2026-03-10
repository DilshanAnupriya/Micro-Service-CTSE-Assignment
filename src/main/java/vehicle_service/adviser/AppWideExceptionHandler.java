package vehicle_service.adviser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vehicle_service.exception.EntryNotFoundException;
import vehicle_service.util.StandardResponseDto;

@RestControllerAdvice
public class AppWideExceptionHandler {
    @ExceptionHandler(EntryNotFoundException.class)
    public ResponseEntity<StandardResponseDto> handleEntyNotFoundException(EntryNotFoundException e){
        return new ResponseEntity<>(
                new StandardResponseDto(404,e.getMessage(),e), HttpStatus.NOT_FOUND
        );
    }
}
