package com.example.medislot.exception;

// Thrown when an entity (Doctor, Patient, Schedule, Appointment) does not exist (HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

