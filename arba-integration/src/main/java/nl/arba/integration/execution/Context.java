package nl.arba.integration.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arba.integration.config.Configuration;
import nl.arba.integration.model.*;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.PatternUtils;
import nl.arba.integration.utils.StreamUtils;
import nl.arba.integration.validation.json.JsonValidator;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public Object evaluate(String expression) {
        if (Pattern.matches(PatternUtils.apiSourceUriParam(), expression)) {
            HttpRequest source = (HttpRequest) getVariable(API_REQUEST);
            return source.getUriParam(expression.substring(expression.lastIndexOf(".")+1));
        }
        else if (Pattern.matches(PatternUtils.createCollection(), expression)) {
            return new Collection();
        }
        else if (Pattern.matches(PatternUtils.createJsonObject(), expression)) {
            return new JsonObject();
        }
        else if (Pattern.matches(PatternUtils.createJsonArray(), expression)) {
            return new JsonArray();
        }
        else if (Pattern.matches(PatternUtils.apiSourceHeader(), expression)) {
            HttpRequest source = (HttpRequest) getVariable(API_REQUEST);
            return source.getHeaders().get(expression.substring(expression.lastIndexOf(".")+1));
        }
        else if (Pattern.matches(PatternUtils.parseJson(), expression)) {
            String jsonExpression = expression.substring(expression.indexOf("(")+1, expression.lastIndexOf(")"));
            Object value = evaluate(jsonExpression);
            if (value instanceof HttpResponse) {
                byte[] jsonbytes = ((HttpResponse) value).getContent();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Map data = mapper.readValue(jsonbytes, Map.class);
                    return JsonObject.fromMap(data);
                }
                catch (Exception err) {
                    try {
                        Map<String,Object>[] array = mapper.readValue(jsonbytes, Map[].class);
                        return JsonArray.fromMaps(array);
                    }
                    catch (Exception err2) {
                        return null;
                    }
                }
            }
            else if (value instanceof byte[]) {
                byte[] jsonbytes = (byte[])value;
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Map data = mapper.readValue(jsonbytes, Map.class);
                    return JsonObject.fromMap(data);
                }
                catch (Exception err) {
                    try {
                        Map<String,Object>[] array = mapper.readValue(jsonbytes, Map[].class);
                        return JsonArray.fromMaps(array);
                    }
                    catch (Exception err2) {
                        return null;
                    }
                }
            }
            return null;
        }
        else if (Pattern.matches(PatternUtils.createHttpRequest(), expression)) {
            HttpRequest request = new HttpRequest();
            String method = expression.substring("new HttpRequest(".length()+1);
            method = method.substring(0, method.indexOf("'"));
            request.setMethod(HttpMethod.fromString(method));
            String urlExpression = expression.substring("new HttpRequest(".length() + method.length()+3);
            urlExpression = urlExpression.substring(0, urlExpression.lastIndexOf(")"));
            String url = (String) evaluate(urlExpression);
            request.setUrl(url);
            return request;
        }
        else if (Pattern.matches(PatternUtils.evalJs(), expression)){
            try {
                ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
                SimpleBindings bindings = new SimpleBindings();
                String initJson = "";
                for (String variable: variables.keySet()) {
                    Object value = variables.get(variable);
                    if (value instanceof JsonArray) {
                        JsonArray array = (JsonArray) value;
                        List<JsonObject> items = array.getItems();
                        Map[] mapItems = new Map[items.size()];
                        for (int index = 0; index < mapItems.length; index++)
                            mapItems[index] = items.get(index).toMap();
                        String json = new ObjectMapper().writeValueAsString(mapItems);
                        initJson += (variable + "=" + json + ";");
                    }
                    else if (value instanceof String) {
                        bindings.put(variable, value);
                    }
                    else if (value instanceof JsonObject) {
                        initJson += (variable + "=" + ((JsonObject) value).toJson() + ";");
                    }
                }
                String jsExpression = initJson + ";" + expression.substring(expression.indexOf("(")+1, expression.lastIndexOf(")")).replaceAll("\n", "");
                return engine.eval(jsExpression, bindings);
            }
            catch (Exception err) {
                err.printStackTrace();
                return null;
            }
        }
        else if (Pattern.matches(PatternUtils.translateJson(), expression)) {
            String[] parameters = expression.substring("translateJson(".length(), expression.lastIndexOf(")")).split(Pattern.quote(","));
            String valueExpression = parameters[0];
            String stylesheetExpression = parameters[1].trim();
            return JsonUtils.translate(StreamUtils.objectToStream(evaluate(valueExpression)),getJsonStylesheets(), (String) evaluate(stylesheetExpression));
        }
        else if (Pattern.matches(PatternUtils.stringLiteral(), expression)) {
            String stringLiteral = expression.substring(1, expression.length()-1);
            Pattern subExpression = Pattern.compile("\\{([^\\}]*)\\}");
            if (JsonUtils.isValidJson(stringLiteral)) {
                return stringLiteral;
            }
            else if (subExpression.matcher(stringLiteral).find()) {
                String result = "";
                int prevpos = 0;
                int pos = stringLiteral.indexOf('{');
                while (pos >= 0) {
                    result += stringLiteral.substring(prevpos, pos);
                    String sub = stringLiteral.substring(pos+1);
                    sub = sub.substring(0, sub.indexOf("}"));
                    String evalResult = (String) evaluate("{"+ sub + "}");
                    result += evalResult;
                    prevpos = stringLiteral.indexOf("}", pos)+1;
                    pos = stringLiteral.indexOf("{", prevpos);
                }
                result += stringLiteral.substring(prevpos);
                return result;
            }
            else {
                return expression.substring(1, expression.length()-1);
            }
        }
        else if (Pattern.matches("\\{([^\\}]*)\\}", expression)) {
            String sub = expression.substring(1, expression.length()-1);
            if (hasVariable(sub))
                return getVariable(sub);
            else if (getConfiguration().hasSetting(sub))
                return getConfiguration().getSetting(sub);
            else if (sub.contains(".")) {
                String[] items = sub.split(Pattern.quote("."));
                Object current = null;
                for (String item: items) {
                    if (current == null)
                        current = evaluate("{" + item + "}");
                    else {
                        if (current instanceof Map) {
                            Map map = (Map) current;
                            if (map.containsKey(item))
                                current = map.get(item);
                        }
                        else if (current instanceof HttpResponse) {
                            HttpResponse response = (HttpResponse) current;
                            if (response.getContentType().startsWith("text/json") || response.getContentType().startsWith("application/json")) {
                                String json = new String(response.getContent());
                                try {
                                    if (json.startsWith("[")) {
                                        current = JsonArray.fromJson(json);
                                    } else {
                                        current = JsonObject.fromJson(json);
                                    }

                                    if (Pattern.matches(PatternUtils.jsonPropertyFilter(), item)) {
                                        current = ((JsonObject) current).evaluateFilter(item);
                                    }
                                    else if (current instanceof JsonObject && ((JsonObject) current).hasProperty(item)) {
                                        current = ((JsonObject) current).getProperty(item);
                                    }
                                }
                                catch (Exception err) {
                                    return false;
                                }
                            }
                            else if (response.getContentType().startsWith("text/html")) {
                                System.out.println("HTML response: " + new String(response.getContent()));
                            }
                        }
                        else if (current instanceof JsonObject) {
                            JsonObject o = (JsonObject) current;
                            if (o.hasProperty(item)) {
                                current = o.getProperty(item);
                            }
                        }
                    }
                }
                return current;
            }
            else
                return null;
        }
        else if (Pattern.matches(PatternUtils.createHttpResponse(), expression)) {
            String code = expression.substring("new HttpResponse(".length());
            code = code.substring(0, code.lastIndexOf(")"));
            return HttpResponse.create(Integer.parseInt(code));
        }
        else
            return null;
    }
}
