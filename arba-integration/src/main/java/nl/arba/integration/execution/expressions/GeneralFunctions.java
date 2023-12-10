package nl.arba.integration.execution.expressions;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.JsonUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeneralFunctions {
    public static boolean isGeneralFunction(String expression) {
        return Pattern.matches(".*" + Pattern.quote("(") + ".*" + Pattern.quote(")"), expression) && !expression.startsWith("new ");
    }

    public static Object evaluate(String expression, Context context) {
        try {
            int index = expression.length() - 1;
            boolean foundOpening = false;
            int nrClose = 0;
            int nrOpen = 0;
            boolean inStringLiteral = false;
            int parameterIndex = index;
            ArrayList<String> parameterExpressions = new ArrayList<>();
            while (index > 0 && !foundOpening) {
                if (expression.charAt(index) == ')')
                    nrClose++;
                else if (expression.charAt(index) == '(')
                    nrOpen++;
                else if (expression.charAt(index) == '\'') {
                    inStringLiteral = !inStringLiteral;
                } else if (expression.charAt(index) == ',' && !inStringLiteral) {
                    parameterExpressions.add(expression.substring(index + 1, parameterIndex).trim());
                    parameterIndex = index;
                }
                foundOpening = (nrClose == nrOpen);
                if (!foundOpening)
                    index--;
            }
            String functionName = expression.substring(0, index).trim();
            parameterExpressions.add(expression.substring(index + 1, parameterIndex).trim());
            Object[] parameterValues = new Object[parameterExpressions.size()+1];
            parameterValues[0] = context;
            Class[] parameterClasses = new Class[parameterExpressions.size()+1];
            parameterClasses[0] = Context.class;
            for (int paramindex = 0; paramindex < parameterExpressions.size(); paramindex++) {
                parameterValues[paramindex+1] = context.evaluate(parameterExpressions.get(paramindex));
                parameterClasses[paramindex+1] = parameterValues[paramindex+1].getClass();
            }
            Method functionMethod = GeneralFunctions.class.getMethod(functionName, parameterClasses);
            return functionMethod.invoke(null, parameterValues);
        }
        catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    public static Object parseJson(Context context, String jsonexpression) throws Exception {
        Object value = context.evaluate(jsonexpression);
        if (value instanceof HttpResponse) {
            byte[] jsonbytes = ((HttpResponse) value).getContent();
            ObjectMapper mapper = JsonUtils.getMapper();
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

    public static Object evalJs(Context context, String expression) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            SimpleBindings bindings = new SimpleBindings();
            String initJson = "";
            for (String variable: context.getVariableNames()) {
                Object value = context.getVariable(variable);
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
            String jsExpression = initJson + ";" + expression.replaceAll("\n", "");
            return engine.eval(jsExpression, bindings);
        }
        catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

}
