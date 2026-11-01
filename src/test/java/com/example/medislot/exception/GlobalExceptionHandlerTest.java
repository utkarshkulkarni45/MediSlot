package com.example.medislot.exception;

import com.example.medislot.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Unique constraint violation maps to 409, not 500")
    void dataIntegrityViolationMapsToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/appointments");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Duplicate entry"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("/appointments", response.getBody().getPath());
    }

    @Test
    @DisplayName("Pessimistic-lock conflict maps to 409")
    void slotAlreadyBookedMapsToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/appointments");

        ResponseEntity<ErrorResponse> response = handler.handleSlotAlreadyBooked(
                new SlotAlreadyBookedException("Slot is already booked"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    @DisplayName("MySQL deadlock or lock timeout maps to 409, not 500")
    void pessimisticLockFailureMapsToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/appointments");

        ResponseEntity<ErrorResponse> response = handler.handlePessimisticLockFailure(
                new PessimisticLockingFailureException("Deadlock found when trying to get lock"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
    }
}
