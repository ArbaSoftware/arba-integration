package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;

import java.awt.*;
import java.util.ArrayList;

public abstract class Step {
    private nl.arba.integration.config.Step config;

    public Step(nl.arba.integration.config.Step config) {
        this.config = config;
    }

    public nl.arba.integration.config.Step getConfiguration() {
        return config;
    }

    public String getName() {
        return getClass().getName().substring(getClass().getName().lastIndexOf(".")+1).toLowerCase();
    }

    public abstract boolean execute(Context context);

    public String[] validate(nl.arba.integration.config.Step config) {
        ArrayList<String> errors = new ArrayList<>();
        for (String param: getRequiredConfigurationParameters()) {
            if (!config.hasSetting(param))
                errors.add("Required parameter '" + param + "' missing for step " + getName());
        }
        return errors.toArray(new String[errors.size()]);
    }

    public abstract String[] getRequiredConfigurationParameters();
}