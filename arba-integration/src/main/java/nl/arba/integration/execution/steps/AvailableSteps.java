package nl.arba.integration.execution.steps;

import nl.arba.integration.config.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AvailableSteps {
    private static HashMap<String, Class> availableSteps;

    public static Map<String, Class> getAvailableSteps() {
        if (availableSteps == null) {
            availableSteps = new HashMap<>();
            InputStream stream = AvailableSteps.class.getClassLoader().getResourceAsStream("nl/arba/integration/execution/steps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            Set<Class> classes = reader.lines()
                    .filter(line -> line.endsWith(".class"))
                    .map(line -> getClass(line, "nl.arba.integration.execution.steps"))
                    .collect(Collectors.toSet());
            for (Class clazz: classes) {
                if (!clazz.equals(AvailableSteps.class) && !clazz.equals(Step.class))
                    availableSteps.put(clazz.getName().substring(clazz.getName().lastIndexOf('.')+1).toLowerCase(), clazz);
            }
        }
        return availableSteps;
    }

    public static Map<String, Class> getAvailableSteps(Configuration config) {
        if (availableSteps == null)
            availableSteps = new HashMap<>();

        for (String classname: config.getStepClasses()) {
            try {
                Class clazz = Class.forName(classname);
                availableSteps.put(clazz.getName().substring(clazz.getName().lastIndexOf('.')+1).toLowerCase(), clazz);
            }
            catch (Exception err) {}
        }
        return availableSteps;
    }

    private static Class getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    public static boolean isValidStep(String name) {
        return getAvailableSteps().containsKey(name.toLowerCase());
    }

    public static Class getStep(String name) {
        return getAvailableSteps().get(name.toLowerCase());
    }
}
