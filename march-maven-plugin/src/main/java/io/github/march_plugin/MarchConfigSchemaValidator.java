package io.github.march_plugin;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a march configuration file against march-config.xsd, packaged as a plugin resource.
 */
public class MarchConfigSchemaValidator {

    private static final String SCHEMA_RESOURCE = "/march-config.xsd";

    /**
     * Validates the given configuration file against march-config.xsd.
     *
     * @param configFile the configuration file to validate.
     * @throws MarchConfigSchemaViolationException if the file does not match the schema, or cannot be parsed as XML.
     */
    public void validate(final File configFile) {
        final var errorHandler = new CollectingErrorHandler();

        try {
            final var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final var schema = schemaFactory.newSchema(getClass().getResource(SCHEMA_RESOURCE));
            final var validator = schema.newValidator();
            validator.setErrorHandler(errorHandler);
            validator.validate(new StreamSource(configFile));
        } catch (final SAXException | IOException e) {
            if (errorHandler.getMessages().isEmpty()) {
                throw new MarchConfigSchemaViolationException(configFile, e);
            }
        }

        if (!errorHandler.getMessages().isEmpty()) {
            throw new MarchConfigSchemaViolationException(configFile, errorHandler.getMessages());
        }
    }

    private static final class CollectingErrorHandler implements ErrorHandler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void warning(final SAXParseException exception) {
            // Ignored: only structural violations fail the build.
        }

        @Override
        public void error(final SAXParseException exception) {
            messages.add(describe(exception));
        }

        @Override
        public void fatalError(final SAXParseException exception) throws SAXException {
            messages.add(describe(exception));
            throw exception;
        }

        List<String> getMessages() {
            return messages;
        }

        private static String describe(final SAXParseException exception) {
            return "Line %d, column %d: %s".formatted(exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage());
        }
    }
}
