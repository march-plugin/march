package io.github.march_plugin.configuration.initializer.exception;

/**
 * Thrown when the root module of the configuration does not define a groupId.
 */
public class MissingRootGroupIdException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public MissingRootGroupIdException() {
        super("The root module must define a groupId.");
    }
}
