package nl.arba.integration.execution;

import nl.arba.integration.config.Configuration;
import nl.arba.integration.config.Step;
import nl.arba.integration.execution.steps.AvailableSteps;
import nl.arba.integration.validation.json.JsonValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Daemon extends Thread {
    private long interval;
    private nl.arba.integration.config.Step[] steps;
    private Configuration config;
    private JsonValidator jsonValidator;
    private Map<String,Object> jsonStylesheets;
    private ArrayList <nl.arba.integration.execution.steps.Step> stepsToExecute;
    private boolean active;

    public Daemon() {
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setSteps(Step[] steps, String[] stepclasses) {
        this.steps = steps;
        stepsToExecute = new ArrayList<>();
        for (Step step: steps) {
            try {
                String stepClass = Arrays.asList(stepclasses).stream().filter(s -> s.toLowerCase().endsWith("." + step.getName())).findFirst().get();
                nl.arba.integration.execution.steps.Step impl = (nl.arba.integration.execution.steps.Step) Class.forName(stepClass).getConstructor(step.getClass()).newInstance(step);
                stepsToExecute.add(impl);
            }
            catch (Exception err) {
                err.printStackTrace();
            } //Not possible because of prior validation
        }
    }

    public String[] validate(String[] stepclasses) {
        ArrayList<String> errors = new ArrayList<>();
        if (steps == null || steps.length == 0)
            errors.add("No steps specified for daemon");
        for (Step step: steps) {
            Optional<String> stepClass = Arrays.asList(stepclasses).stream().filter(s -> s.toLowerCase().endsWith("." + step.getName())).findFirst();
            if (!stepClass.isPresent())
                errors.add("Step '" + step.getName() + "' has invalid step type");
            else {
                try {
                    Object stepInstance = Class.forName(stepClass.get()).getConstructor(Step.class).newInstance(step);
                    errors.addAll(Arrays.asList(((nl.arba.integration.execution.steps.Step) stepInstance).validate(step)));
                }
                catch (Exception err) {
                    errors.add("Unexpected error on instantiating step " + step.getName() + " : " + err.getMessage());
                }
            }
        }
        return errors.toArray(new String[errors.size()]);
    }

    private void execute() {
        Context context = Context.create(config, jsonValidator, jsonStylesheets);
        boolean succeeded = true;
        for (nl.arba.integration.execution.steps.Step step: stepsToExecute) {
            boolean result = step.execute(context);
            if (!result) {
                succeeded = false;
                break;
            }
        }
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    public void setJsonValidator(JsonValidator validator) {
        jsonValidator = validator;
    }

    public void setJsonStylesheets(Map<String,Object> stylesheets) {
        jsonStylesheets = stylesheets;
    }

    public void run() {
        active = true;
        while (active) {
            execute();
            try {
                sleep(interval);
            }
            catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public void halt() {
        active = false;
    }

}
