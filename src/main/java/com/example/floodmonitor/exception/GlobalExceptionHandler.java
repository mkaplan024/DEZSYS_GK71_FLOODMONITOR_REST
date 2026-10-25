package com.example.floodmonitor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// zentrale Fehlerbehandlung für alle Controller
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Station gibt es nicht
    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStationNotFound(StationNotFoundException e, HttpServletRequest request) {
        return createResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    // 400 - ungültige Eingaben aus dem StationService (Zeitraum, limit)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return createResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // 400 - Parameter hat den falschen Typ (z. B. warningLevel=HOCH, from=gestern, limit=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Ungültiger Wert '" + e.getValue() + "' für Parameter '" + e.getName() + "'";

        if (e.getName().equals("warningLevel")) {
            message += ". Erlaubt sind: NORMAL, WARNING, CRITICAL, UNKNOWN";
        }
        if (e.getName().equals("from") || e.getName().equals("to")) {
            message += ". Erwartetes Format: 2026-05-01T08:00:00Z";
        }

        return createResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // 404 - URL gibt es nicht
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        return createResponse(HttpStatus.NOT_FOUND, "Der Pfad " + request.getRequestURI() + " existiert nicht", request);
    }

    // 405 - z. B. POST auf einen GET-Endpunkt
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return createResponse(HttpStatus.METHOD_NOT_ALLOWED, "Die Methode " + e.getMethod() + " wird nicht unterstützt", request);
    }

    // 415 - Medientyp im Request wird nicht unterstützt
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return createResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Der Medientyp wird nicht unterstützt", request);
    }

    // 406 - gewünschtes Format wird nicht unterstützt
    // hier wird die Antwort fix als JSON geschickt, weil der Client ja kein unterstütztes Format angegeben hat
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                HttpStatus.NOT_ACCEPTABLE.value(),
                HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(),
                "Format nicht unterstützt. Erlaubt sind application/json und application/xml",
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    // 500 - alle anderen Fehler, ohne Stacktrace in der Antwort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e, HttpServletRequest request) {
        System.out.println("Interner Fehler: " + e.getMessage()); // nur in der Server-Konsole
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ein interner Fehler ist aufgetreten", request);
    }

    private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());

        return ResponseEntity.status(status).body(error);
    }
}