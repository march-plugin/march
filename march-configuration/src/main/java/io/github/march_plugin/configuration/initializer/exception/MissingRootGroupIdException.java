package io.github.march_plugin.configuration.initializer.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the root module of the configuration does not define a groupId.
 */
public class MissingRootGroupIdException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public MissingRootGroupIdException() {
        super("The root module must define a groupId.");
    }
}
