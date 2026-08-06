package io.github.march_plugin.core.enforcement.project.exceptions;

/**
 * Thrown when the analysis of the project throws an IO exception.
 */
public class ProjectAnalysisIOException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param path the path, where the IOException occurred.
     */
    public ProjectAnalysisIOException(final String path) {
        super("An IO exception occurred in project analysis at '%s'".formatted(path));
    }
}