package com.example.medislot.exception;

// Thrown when a slot is not on the interval grid, outside working hours, or in the past (HTTP 400 Bad Request)
public class InvalidSlotException extends RuntimeException {

    public InvalidSlotException(String message) {
        super(message);
    }
}

