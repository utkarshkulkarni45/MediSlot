package com.utkarsh.medislot.exception;

public class PatientEmailAlreadyExistsException extends RuntimeException {

    public PatientEmailAlreadyExistsException(String message) {
        super(message);
    }
}
