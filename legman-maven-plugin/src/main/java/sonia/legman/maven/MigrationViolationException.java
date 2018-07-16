package sonia.legman.maven;

public class MigrationViolationException extends RuntimeException {

    public MigrationViolationException(String message) {
        super(message);
    }
}
