package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.execution.expressions.InvalidExpressionException;
import nl.arba.integration.model.HttpRequest;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.PatternUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Content extends Step {
    private String contentType;
    private String contentExpression;
    private String propertyName;

    public Content(nl.arba.integration.config.Step stepconfig) {
        super(stepconfig);
        contentType = stepconfig.getSetting("contenttype").toString();
        contentExpression = stepconfig.getSetting("content").toString();
        propertyName = stepconfig.getSetting("property").toString();
    }
    @Override
    public boolean execute(Context context) {
        try {
            Object contentValue = context.evaluate(contentExpression);
            byte[] bytesValue = new byte[0];
            if (contentValue instanceof String)
                bytesValue = ((String) contentValue).getBytes();
            else if (contentValue instanceof JsonArray) {
                JsonArray array = (JsonArray) contentValue;
                String json = "[" + array.getItems().stream().map(o -> o.toJson()).collect(Collectors.joining(",")) + "]";
                bytesValue = json.getBytes();
            } else if (contentValue instanceof JsonObject) {
                bytesValue = ((JsonObject) contentValue).toJson().getBytes();
            } else if (contentValue instanceof HttpResponse) {
                bytesValue = ((HttpResponse) contentValue).getContent();
            } else if (contentValue instanceof byte[]) {
                bytesValue = (byte[]) contentValue;
            }
            Object propertyValue = context.getVariable(propertyName.equals("api.response") ? Context.API_RESPONSE : propertyName);
            if (propertyValue instanceof HttpResponse) {
                HttpResponse target = (HttpResponse) propertyValue;
                target.setContent(bytesValue);
                target.setContentType(contentType);
            } else if (propertyValue instanceof HttpRequest) {
                HttpRequest target = (HttpRequest) propertyValue;
                target.setPostBody(bytesValue);
                target.setContentType(contentType);
            }
            return true;
        }
        catch (InvalidExpressionException e) {
            return false;
        }
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"property", "contenttype", "content"};
    }
}
