package nl.arba.integration.config;

import nl.arba.integration.utils.ImageUtils;

import java.awt.*;
import java.util.Map;

public class Step {
    private String name;
    private Map<String, Object> settings;
    private Step[] subSteps;

    private Step[] elseSteps;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSettings(Map <String, Object> settings) {
        this.settings = settings;
    }

    public Map <String, Object> getSettings() {
        return settings;
    }

    public boolean hasSetting(String name) {
        return this.settings.containsKey(name);
    }

    public Object getSetting(String name) {
        return settings.get(name);
    }

    public void setSteps(Step[] steps) {
        subSteps = steps;
    }

    public Step[] getSteps() {
        return subSteps;
    }

    public void setElsesteps(Step[] steps) {
        elseSteps = steps;
    }

    public Step[] getElseSteps() {
        return elseSteps;
    }

    public int paint(Graphics g, int x, int y) {
        if (getSteps() == null || getSteps().length == 0) {
            g.drawRect(x, y - 8, 32, 32);
            return 32;
        }
        else {
            int width = 10;
            int currentx = x+10;
            int currenty = y;
            boolean first = true;
            for (Step step: getSteps()) {
                int stepwidth = step.paint(g, currentx,currenty);
                if (!first)
                    ImageUtils.drawArrowLine(g, currentx-ImageUtils.getSpaceBetweenSteps(), currenty+8, currentx, currenty+8, 5, 5);
                currentx += stepwidth+ImageUtils.getSpaceBetweenSteps();
                width += (stepwidth+ImageUtils.getSpaceBetweenSteps());
                first = false;
            }
            width+=10;
            width-=ImageUtils.getSpaceBetweenSteps();
            g.drawRect(x, y-18, width, 52);
            return width;
        }
    }
}
