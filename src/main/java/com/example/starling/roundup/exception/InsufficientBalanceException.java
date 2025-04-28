package com.example.starling.roundup.exception;

public class InsufficientBalanceException extends RuntimeException {
  public InsufficientBalanceException(String message) {
      super(message);
  }
}
