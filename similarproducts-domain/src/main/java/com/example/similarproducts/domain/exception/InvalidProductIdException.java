package com.example.similarproducts.domain.exception;

public class InvalidProductIdException extends RuntimeException {

    public InvalidProductIdException(String message) {
        super(message);
    }
}

