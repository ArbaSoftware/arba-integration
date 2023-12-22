package nl.arba.integration.config;

import nl.arba.integration.execution.steps.AvailableSteps;
import nl.arba.integration.model.HttpMethod;
import nl.arba.integration.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Api {
    private String uriPattern;
    private ArrayList<HttpMethod> methods = new ArrayList<>();
    private Step[] steps;
    private boolean authorizationRequired = false;

    public void setUripattern(String pattern) {
        uriPattern = pattern;
    }

    public String getUriPattern() {
        return uriPattern;
    }

    public void setMethods(String[] methods) {
        for (String method: methods) {
            this.methods.add(HttpMethod.fromString(method));
        }
    }

    public List<HttpMethod> getMethods() {
        return methods;
    }

    public void setSteps(Step[] steps) {
        this.steps = steps;
    }

    public Step[] getSteps() {
        return steps;
    }

    public String[] validate(String[] stepclasses) {
        ArrayList<String> errors = new ArrayList<>();
        if (methods.isEmpty())
            errors.add("No method specified for api");
        if (steps == null || steps.length == 0)
            errors.add("No steps specified for api");
        for (Step step: getSteps()) {
            if (!Arrays.asList(stepclasses).stream().filter(s -> s.toLowerCase().endsWith("." + step.getName())).findFirst().isPresent())
                errors.add("Step '" + step.getName() + "' has invalid step type");
            else {
                try {
                    Object stepInstance = AvailableSteps.getStep(step.getName()).getConstructor(Step.class).newInstance(step);
                    errors.addAll(Arrays.asList(((nl.arba.integration.execution.steps.Step) stepInstance).validate(step)));
                }
                catch (Exception err) {
                    errors.add("Unexpected error on instantiating step " + step.getName() + " : " + err.getMessage());
                }
            }

        }
        return errors.toArray(new String[errors.size()]);
    }

    public void setAuthorization(String value) {
        authorizationRequired = value.equalsIgnoreCase("required");
    }

    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    public RenderedImage createImage() {
        int totalWidth = 0;
        boolean first = true;
        for (Step step: getSteps()) {
            totalWidth += (getStepWidth(step) + (first ? 0 : ImageUtils.getSpaceBetweenSteps()));
            first = false;
        }
        BufferedImage result = new BufferedImage((totalWidth*2) + 40, 100, BufferedImage.TYPE_INT_RGB);
        Graphics g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0, (2*totalWidth)+40, 100);
        g.setColor(Color.BLACK);
        int x = 20;
        int y = 50;
        first = true;
        for (Step step: getSteps()) {
            int stepwidth = step.paint(g, x,y);
            if (!first)
                ImageUtils.drawArrowLine(g, x-ImageUtils.getSpaceBetweenSteps(), y+8, x, y+8, 5, 5);
            x += stepwidth+ImageUtils.getSpaceBetweenSteps();
            first = false;
        }
        return result;
    }

    private int getStepWidth(Step step) {
        int result = 32;
        if (step.getSteps() != null && step.getSteps().length > 0) {
            for (Step child: step.getSteps())
                result += (getStepWidth(child)+15);
        }
        return result;
    }
}
