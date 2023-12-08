package nl.arba.integration.validation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ValidationException extends Exception {
    public ValidationException(String[] errors) {
        super(Arrays.asList(errors).stream().collect(Collectors.joining(";")));
    }
}
