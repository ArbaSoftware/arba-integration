package nl.arba.integration.config;

import nl.arba.integration.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class Configuration {
    //private static Configuration onlyInstance;
    private Api[] apis;
    private Map<String, Object> settings;
    private String[] stepClasses;
    private Daemon[] daemons;
    private Bean[] beans = new Bean[0];

    public static Configuration create(InputStream source) throws IOException {
        return JsonUtils.getMapper().readValue(source, Configuration.class);
    }

    public void setApis(Api[] apis) {
        this.apis = apis;
    }

    public Api[] getApis() {
        return apis;
    }

    public void setSettings(Map<String, Object> values) {
        settings = values;
    }

    public boolean hasSetting(String name) {
        return settings != null && settings.containsKey(name);
    }

    public Object getSetting(String name) {
        return settings.get(name);
    }

    public String[] getSettings() {
        return settings.keySet().toArray(new String[0]);
    }

    public String[] validate() {
        ArrayList <String> errors = new ArrayList<>();
        if (getApis() == null || getApis().length == 0)
            errors.add("No api's defined");
        for (Api api: getApis()) {
            errors.addAll(Arrays.asList(api.validate()));
        }
        return errors.toArray(new String[errors.size()]);
    }

    public void setStepclasses(String[] classes) {
        stepClasses = classes;
    }

    public String[] getStepClasses() {
        return stepClasses;
    }

    public void setDaemons(Daemon[] daemons) {
        this.daemons = daemons;
    }

    public Daemon[] getDaemons() {
        return daemons;
    }

    public void setBeans(Bean[] beans) {
        this.beans = beans;
    }

    public Bean[] getBeans() {
        return beans;
    }

    public Bean getBean(String name) {
        if (beans == null)
            return null;
        else
            return Arrays.asList(beans).stream().filter(b -> b.getName().equals(name)).findFirst().get();
    }

    public boolean hasBean(String name) {
        if (beans == null)
            return false;
        boolean found = false;
        for (Bean b: beans) {
            if (b.getName().equals(name)) {
                found = true;
                break;
            }
        }
        return found;
    }
}
