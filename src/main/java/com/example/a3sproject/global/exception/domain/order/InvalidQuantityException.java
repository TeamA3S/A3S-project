package com.example.a3sproject.global.exception.domain.order;

import static org.eclipse.jdt.internal.compiler.parser.Scanner.INVALID_INPUT;

public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException() {
        super(INVALID_INPUT);
    }
}
