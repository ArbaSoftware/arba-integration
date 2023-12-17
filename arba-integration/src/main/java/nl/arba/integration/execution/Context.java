package nl.arba.integration.execution;

import nl.arba.integration.config.Configuration;
import nl.arba.integration.execution.expressions.Expression;
import nl.arba.integration.execution.expressions.InvalidExpressionException;
import nl.arba.integration.validation.json.JsonValidator;

import java.util.HashMap;
import java.util.Map;

public class Context {
    public static final String API_REQUEST = "api.request";
    public static final String API_REQUEST_BODY = "api.request.body";
    public static final String API_RESPONSE = "api.response";

    private HashMap<String, Object> variables  = new HashMap<>();
    private Configuration configuration;
    private JsonValidator jsonValidator;
    private Map<String,Object> jsonStylesheets;

    private Context(Configuration config, JsonValidator jsonvalidator, Map<String,Object> jsonstylesheets) {
        configuration = config;
        this.jsonValidator = jsonvalidator;
        this.jsonStylesheets = jsonstylesheets;
    }

    public static Context create(Configuration config, JsonValidator jsonvalidator, Map<String,Object> jsonstylesheets) {
        return new Context(config, jsonvalidator, jsonstylesheets);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public Object getVariable(String name) {
        return variables.get(translateVariableName(name));
    }

    public String[] getVariableNames() {
        return variables.keySet().toArray(new String[0]);
    }

    private String translateVariableName(String name) {
        if ("api.source".equals(name))
            return API_REQUEST;
        else if ("api.response".equals(name))
            return API_RESPONSE;
        else
            return name;
    }

    public JsonValidator getJsonValidator() {
        return jsonValidator;
    }

    public Map<String,Object> getJsonStylesheets() {
        return this.jsonStylesheets;
    }

    public Object evaluate(String expression) throws InvalidExpressionException {
        return Expression.evaluate(this, expression);
    }
}
