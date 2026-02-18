package ru.tecius.telemed.processor.error;

import static javax.tools.Diagnostic.Kind.ERROR;

import javax.annotation.processing.Messager;

public final class ErrorHandler {

  private final Messager messager;

  public ErrorHandler(Messager messager) {
    this.messager = messager;
  }

  public void reportError(String message) {
    messager.printMessage(ERROR, message);
  }

}
