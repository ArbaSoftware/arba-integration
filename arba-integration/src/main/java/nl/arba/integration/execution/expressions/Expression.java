package nl.arba.integration.execution.expressions;

import netscape.javascript.JSObject;
import nl.arba.integration.execution.Context;
import nl.arba.integration.model.ArrayValue;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.JsonUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Expression {
    public static Object evaluate(Context context, String expression) throws InvalidExpressionException {
        System.out.println("Evaluate: " + expression);
        if (isNumeric(expression)) {
            return Integer.parseInt(expression);
        }
        else if (isStringLiteral(expression)) {
            String stringLiteral = expression.substring(1, expression.lastIndexOf("'"));
            int searchpos = 0;
            while (stringLiteral.indexOf("{", searchpos) >= 0 && stringLiteral.indexOf("}", searchpos) > 0 && stringLiteral.indexOf("{", searchpos) < stringLiteral.indexOf( "}", searchpos)) {
                int index = stringLiteral.indexOf("{", searchpos);
                int endindex = stringLiteral.indexOf("}", index);
                String variableExpression = stringLiteral.substring(index, endindex+1);
                try {
                    Object variableValue = evaluate(context, variableExpression);
                    if (variableValue instanceof String) {
                        stringLiteral = stringLiteral.substring(0, index) + variableValue + stringLiteral.substring(endindex + 1);
                    }
                }
                catch (Exception err) {}
                searchpos = (endindex+1);
            }
            return stringLiteral;
        }
        else if (isVariable(expression)) {
            String variableName = expression.substring(1, expression.lastIndexOf("}"));
            if (context.hasVariable(variableName))
                return context.getVariable(variableName);
            else if (context.getConfiguration().hasSetting(variableName)) {
                return context.getConfiguration().getSetting(variableName);
            }
            else if (variableName.contains(".")) {
                String[] parts = variableName.split(Pattern.quote("."));
                Object currentValue = evaluate(context, "{" + parts[0] + "}");
                for (int index = 1; index < parts.length; index++) {
                    if (currentValue instanceof JsonObject)
                        currentValue = ((JsonObject) currentValue).getProperty(parts[index]);
                    else if (currentValue instanceof Map)
                        currentValue = ((Map) currentValue).get(parts[index]);
                    else if (currentValue instanceof HttpResponse) {
                        try {
                            byte[] content = ((HttpResponse) currentValue).getContent();
                            Map jsonInput = JsonUtils.getMapper().readValue(((HttpResponse) currentValue).getContent(), Map.class);
                            currentValue = jsonInput.get(parts[index]);
                        }
                        catch (Exception err) {
                            throw new InvalidExpressionException(expression);
                        }
                    }
                }
                return currentValue;
            }
            else
                throw new InvalidExpressionException(expression);
        }
        else if (expression.startsWith("new ")) {
            String classname = expression.substring(4).trim();
            if (classname.contains("(") && classname.endsWith(")")) {
                classname = classname.substring(0, classname.indexOf("("));
                Object[] parameterValues = getParameterValues(context, expression.substring(expression.indexOf("(")+1, expression.lastIndexOf(")")));
                try {
                    Class targetClass = Class.forName("nl.arba.integration.model." + classname);
                    Constructor targetConstructor = targetClass.getConstructor(getParameterClasses(parameterValues));
                    return targetConstructor.newInstance(parameterValues);
                }
                catch (Exception err) {
                    throw new InvalidExpressionException(expression);
                }
            }
            else
                throw new InvalidExpressionException(expression);
        }
        else if (isBeanCall(context, expression)) {
            return null;
        }
        else if (isObjectMethodCall(context, expression)) {
            return null;
        }
        else if (isFunctionCall(expression)) {
            try {
                String functionName = expression.substring(0, expression.indexOf("("));
                Object[] parameterValues = getParameterValues(context, expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")")));
                Object[] includedParameterValues = new Object[parameterValues.length+1];
                includedParameterValues[0] = context;
                for (int index = 0; index < parameterValues.length; index++)
                    includedParameterValues[index+1] = parameterValues[index];
                Class[] parameterClasses = getParameterClasses(includedParameterValues);
                Method tocall = GeneralFunctions.class.getMethod(functionName, parameterClasses);
                return tocall.invoke(null, includedParameterValues);
            }
            catch (Exception err) {
                throw new InvalidExpressionException(expression);
            }
        }
        else {
            Object currentValue = null;
            String currentExpression = "";
            String currentFunctionName = "";
            boolean inParameterExpression = false;
            String parameterExpression = "";
            int nrOpenHaakjes = 1;
            int nrSluitHaakjes = 0;
            boolean inIndexExpression = false;
            String currentIndexExpression = "";
            for (int index = 0; index < expression.length(); index++) {
                if (expression.charAt(index) == '.') {
                    if (inParameterExpression)
                        parameterExpression += ".";
                    else if (!currentExpression.isEmpty()){
                        currentValue = evaluate(context, currentExpression);
                        currentExpression = ".";
                    }
                    else
                        currentExpression += '.';
                }
                else if (expression.charAt(index) == '(') {
                    if (inParameterExpression) {
                        nrOpenHaakjes++;
                        parameterExpression += "(";
                    }
                    else {
                        currentFunctionName = currentExpression;
                        inParameterExpression = true;
                        nrOpenHaakjes = 1;
                        nrSluitHaakjes = 0;
                    }
                }
                else if (expression.charAt(index) == ')') {
                    if (inParameterExpression) {
                        nrSluitHaakjes++;
                        if (nrSluitHaakjes == nrOpenHaakjes) {
                            try {
                                Object[] parameterValues = getParameterValues(context, parameterExpression);
                                Class[] parameterClasses = getParameterClasses(parameterValues);
                                if (currentFunctionName.startsWith("."))
                                    currentFunctionName = currentFunctionName.substring(1);
                                Method m = currentValue.getClass().getMethod(currentFunctionName, parameterClasses);
                                currentValue = m.invoke(currentValue, parameterValues);
                                currentExpression = "";
                            }
                            catch (Exception err) {
                                throw new InvalidExpressionException(expression);
                            }
                            inParameterExpression = false;
                        }
                        else {
                            parameterExpression += ")";
                        }
                    }
                }
                else if (inParameterExpression) {
                    parameterExpression += expression.charAt(index);
                }
                else if (expression.charAt(index) == '[' && !inParameterExpression) {
                    inIndexExpression = true;
                }
                else if (expression.charAt(index) == ']' && !inParameterExpression) {
                    try {
                        inIndexExpression = false;
                        int arrayIndex = (Integer) evaluate(context, currentIndexExpression);
                        if (currentValue instanceof ArrayValue)
                            currentValue = ((ArrayValue) currentValue).get(arrayIndex);
                    }
                    catch (Exception err) {
                        throw new InvalidExpressionException(expression);
                    }
                }
                else if (inIndexExpression) {
                    currentIndexExpression += expression.charAt(index);
                }
                else {
                    currentExpression += expression.charAt(index);
                }
            }
            if (currentExpression.startsWith(".")) {
                if (currentValue instanceof JsonObject)
                    currentValue = ((JsonObject) currentValue).getProperty(currentExpression.substring(1));
                else if (currentValue instanceof Map)
                    currentValue = ((Map) currentValue).get(currentExpression.substring(1));
            }
            return currentValue;
        }
        /*
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
        else if (GeneralFunctions.isGeneralFunction(expression)) {
            return GeneralFunctions.evaluate(expression, this);
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
        else if (Pattern.matches("(.*)" + Pattern.quote(".") + "(.*)" + Pattern.quote("(") + ".*" + Pattern.quote(")"), expression)) {
            System.out.println("Bean call");
            return null;
        }
        else
            return null;
        */
    }

    private static Object[] getParameterValues(Context context, String parameterexpression) throws InvalidExpressionException{
        ArrayList<String> parameterExpressions = new ArrayList<>();
        String cleanedParameterExpression = parameterexpression.trim();
        String currentExpression = "";
        boolean inStringLiteral = false;
        for (int index = 0; index < cleanedParameterExpression.length(); index++) {
            if (cleanedParameterExpression.charAt(index) == ',' && !inStringLiteral) {
                parameterExpressions.add(currentExpression.toString().trim());
                currentExpression = "";
            }
            else if (cleanedParameterExpression.charAt(index) == '\'') {
                inStringLiteral = !inStringLiteral;
                currentExpression += '\'';
            }
            else {
                currentExpression += cleanedParameterExpression.charAt(index);
            }
        }
        if (!currentExpression.isEmpty())
            parameterExpressions.add(currentExpression.trim());

        Object[] results = new Object[parameterExpressions.size()];
        for (int index = 0; index < results.length; index++)
            results[index] = evaluate(context, parameterExpressions.get(index));
        return results;
    }

    private static Class[] getParameterClasses(Object[] values) {
        return Arrays.asList(values).stream().map(v -> v.getClass()).collect(Collectors.toList()).toArray(new Class[0]);
    }

    private static boolean isNumeric(String expression) {
        try {
            Integer.parseInt(expression);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static boolean isStringLiteral(String expression) {
        return expression.trim().startsWith("'") && expression.trim().endsWith("'");
    }

    private static boolean isBeanCall(Context context, String expression) {
        if (expression.contains(".") && expression.contains("(") && expression.endsWith(")")) {
            String beanName = expression.substring(0, expression.indexOf("."));
            if (context.getConfiguration().hasBean(beanName)) {
                String functionName = expression.substring(beanName.length() + 1);
                if (functionName.contains("(")) {
                    functionName = functionName.substring(0, functionName.indexOf('('));
                    if (functionName.contains("."))
                        return false;
                    else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isObjectMethodCall(Context context, String expression) {
        if (expression.contains(".") && expression.contains("(") && expression.endsWith(")")) {
            String objectName = expression.substring(0, expression.indexOf("."));
            if (context.hasVariable(objectName)) {
                String functionName = expression.substring(objectName.length() + 1);
                if (functionName.contains("(")) {
                    functionName = functionName.substring(0, functionName.indexOf('('));
                    if (functionName.contains("."))
                        return false;
                    else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isVariable(String expression) {
        return expression.startsWith("{") && expression.endsWith("}");
    }

    private static boolean isFunctionCall(String expression) {
        if (Pattern.matches(".*" + Pattern.quote("(") + ".*" + Pattern.quote(")"), expression)) {
            String functionName = expression.substring(0, expression.indexOf("("));
            if (functionName.contains("."))
                return false;
            else {
                return true;
            }
        }
        else
            return false;
    }
}
