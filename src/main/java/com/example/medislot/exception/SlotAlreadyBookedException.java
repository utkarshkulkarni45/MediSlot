package com.example.medislot.exception;

// Thrown when attempting to book a slot that has already been reserved (HTTP 409 Conflict)
public class SlotAlreadyBookedException extends RuntimeException {

    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}

