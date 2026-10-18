package com.bankingsystem.exception;

/**
 * Thrown when a requested entity is not found or not owned by the caller.
 * Leaking existence of unauthorized resources is prevented by returning 404.
 */
public class ResourceNotFoundException extends RuntimeException {

  private final String resource;
  private final Object identifier;

  public ResourceNotFoundException(String resource, Object identifier) {
    super(String.format("%s with identifier '%s' was not found", resource, identifier));
    this.resource = resource;
    this.identifier = identifier;
  }

  public ResourceNotFoundException(String message) {
    super(message);
    this.resource = "Resource";
    this.identifier = null;
  }

  public String getResource() {
    return resource;
  }

  public Object getIdentifier() {
    return identifier;
  }
}
