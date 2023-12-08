package nl.arba.integration.validation.json;

import nl.arba.integration.validation.ValidationException;

import java.io.InputStream;

public class JsonValidator {
    private JsonSchema schemas;

    private JsonValidator(JsonSchema schemas) {
        this.schemas = schemas;
    }

    public static JsonValidator create(JsonSchema schema) {
        return new JsonValidator(schema);
    }

    public void validate(InputStream json, String schema) throws ValidationException {
        String[] errors = schemas.validate(schema, json);
        if (errors.length > 0)
            throw new ValidationException(errors);
    }
}
