package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpMethod;
import nl.arba.integration.model.HttpRequest;
import nl.arba.integration.model.HttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.ArrayList;
import java.util.Map;

public class HttpCall extends Step {
    private String requestVariable;
    private String responseVariable;
    public HttpCall(nl.arba.integration.config.Step config) {
        super(config);
        requestVariable = config.getSetting("request").toString();
        responseVariable = config.getSetting("response").toString();
    }

    @Override
    public boolean execute(Context context) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpRequest request = (HttpRequest) context.getVariable(requestVariable);
        if (request.getMethod().equals(HttpMethod.GET)) {
            HttpGet get = new HttpGet(request.getUrl());
            Map<String,String> headers = request.getHeaders();
            for (String header: headers.keySet()) {
                get.addHeader(header, headers.get(header));
            }
            try {
                CloseableHttpResponse response = client.execute(get);
                context.setVariable(responseVariable.equals("api.response")? Context.API_RESPONSE: responseVariable, HttpResponse.from(response));
                return true;
            }
            catch (Exception err) {
                return false;
            }
        }
        else if (request.getMethod().equals(HttpMethod.POST)) {
            HttpPost post = new HttpPost(request.getUrl());
            Map<String,String> headers = request.getHeaders();
            for (String header: headers.keySet()) {
                post.addHeader(header, headers.get(header));
            }
            if ("text/json".equals(request.getContentType())) {
                System.out.println("Post request: " + new String(request.getPostBody()));
                post.setEntity(new ByteArrayEntity(request.getPostBody(), ContentType.APPLICATION_JSON, false));
            }
            try {
                CloseableHttpResponse response = client.execute(post);
                context.setVariable(responseVariable.equals("api.response")? Context.API_RESPONSE: responseVariable, HttpResponse.from(response));
                System.out.println("Post request: " + response.getCode());
                return true;
            }
            catch (Exception err) {
                return false;
            }
        }
        else
            return false;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"request","response"};
    }
}
