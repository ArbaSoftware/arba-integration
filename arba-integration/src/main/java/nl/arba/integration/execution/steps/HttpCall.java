package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpMethod;
import nl.arba.integration.model.HttpRequest;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                err.printStackTrace();
                return false;
            }
        }
        else if (request.getMethod().equals(HttpMethod.POST)) {
            HttpPost post = new HttpPost(request.getUrl());
            Map<String,String> headers = request.getHeaders();
            for (String header: headers.keySet()) {
                post.addHeader(header, headers.get(header));
            }
            System.out.println("Content type:" + request.getContentType() + "/" + ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            if ("text/json".equals(request.getContentType())) {
                post.setEntity(new ByteArrayEntity(request.getPostBody(), ContentType.APPLICATION_JSON, false));
            }
            else if ("application/x-www-form-urlencoded".equals(request.getContentType())) {
                try {
                    Map<String, String> formValues = JsonUtils.getMapper().readValue(request.getPostBody(), Map.class);
                    System.out.println("Namevalues: " + formValues);
                    ArrayList<NameValuePair> values = new ArrayList<>();
                    values.addAll(formValues.keySet().stream().map(k -> new BasicNameValuePair(k, formValues.get(k))).collect(Collectors.toList()));
                    post.setEntity(new UrlEncodedFormEntity(values));
                }
                catch (Exception err) {}
            }
            try {
                CloseableHttpResponse response = client.execute(post);
                context.setVariable(responseVariable.equals("api.response")? Context.API_RESPONSE: responseVariable, HttpResponse.from(response));
                System.out.println("Post request: " + response.getCode());
                return true;
            }
            catch (Exception err) {
                err.printStackTrace();
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
