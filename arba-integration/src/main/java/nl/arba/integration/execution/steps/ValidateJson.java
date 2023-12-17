package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.execution.expressions.InvalidExpressionException;
import nl.arba.integration.validation.ValidationException;

import java.io.ByteArrayInputStream;

public class ValidateJson extends Step {
    public ValidateJson(nl.arba.integration.config.Step config) {
        super(config);
    }

    @Override
    public boolean execute(Context context) {
        try {
            Object json = context.evaluate(getConfiguration().getSetting("json").toString());
            if (json instanceof byte[]) {
                try {
                    context.getJsonValidator().validate(new ByteArrayInputStream((byte[]) json), getConfiguration().getSetting("schema").toString());
                    return true;
                } catch (ValidationException ve) {
                    ve.printStackTrace();
                    return false;
                }
            }
            return false;
        }
        catch (InvalidExpressionException e) {
            return false;
        }
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {
                "json", "schema"
        };
    }
}
