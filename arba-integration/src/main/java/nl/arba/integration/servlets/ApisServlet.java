package nl.arba.integration.servlets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.arba.integration.config.Configuration;
import nl.arba.integration.execution.Api;
import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpMethod;
import nl.arba.integration.model.HttpRequest;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.utils.HttpUtils;
import nl.arba.integration.utils.StreamUtils;
import nl.arba.integration.validation.json.JsonValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApisServlet extends HttpServlet {
    private Configuration config;
    private ArrayList<Api> apis;
    private JsonValidator jsonValidator;
    private Map<String,Object> jsonStylesheets;

    public ApisServlet(Configuration config, JsonValidator jsonvalidator, Map<String,Object> jsonstylesheets) {
        this.config = config;
        this.jsonValidator = jsonvalidator;
        this.jsonStylesheets = jsonstylesheets;
        apis = new ArrayList<>();
        for (nl.arba.integration.config.Api api : config.getApis()) {
            apis.add(Api.create(api, config));
        }
    }

    private List<Api> getApis() {
        return apis;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        Optional<Api> potentialApi = getApis().stream().filter(a -> a.handles(uri) && a.supports(HttpMethod.fromString("get"))).findFirst();

        if (potentialApi.isPresent()) {
            Api handler = potentialApi.get();
            if (handler.isAuthorizationRequired() && request.getHeader("Authorization") == null) {
                HttpUtils.forbidden(response);
            }
            else {
                Context context = Context.create(config, jsonValidator, jsonStylesheets);
                context.setVariable(Context.API_REQUEST,handler.createSource(request));
                Map<String, String> urlParameters = handler.readParametersFromUrl(uri);
                urlParameters.keySet().stream().forEach(k -> context.setVariable(k, urlParameters.get(k)));
                boolean result = handler.execute(request, response, context);
                if (result) {
                    HttpResponse responze = (HttpResponse) context.getVariable(Context.API_RESPONSE);
                    HttpUtils.sendResponse(responze, response);
                } else
                    HttpUtils.failure(response);
            }
        }
        else {
            HttpUtils.notFound(response);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        Optional<Api> potentialApi = getApis().stream().filter(a -> a.handles(uri) && a.supports(HttpMethod.fromString("post"))).findFirst();

        if (potentialApi.isPresent()) {
            Api handler = potentialApi.get();
            if (handler.isAuthorizationRequired() && request.getHeader("Authorization") == null) {
                HttpUtils.forbidden(response);
            }
            else {
                Context context = Context.create(config, jsonValidator, jsonStylesheets);
                context.setVariable(Context.API_REQUEST,handler.createSource(request));
                context.setVariable(Context.API_REQUEST_BODY, ((HttpRequest) context.getVariable(Context.API_REQUEST)).getPostBody() );
                Map<String, String> urlParameters = handler.readParametersFromUrl(uri);
                urlParameters.keySet().stream().forEach(k -> context.setVariable(k, urlParameters.get(k)));
                boolean result = handler.execute(request, response, context);
                if (result) {
                    HttpResponse responze = (HttpResponse) context.getVariable(Context.API_RESPONSE);
                    HttpUtils.sendResponse(responze, response);
                } else
                    HttpUtils.failure(response);
            }
        }
        else {
            HttpUtils.notFound(response);
        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        Optional<Api> potentialApi = getApis().stream().filter(a -> a.handles(uri) && a.supports(HttpMethod.fromString("post"))).findFirst();

        if (potentialApi.isPresent()) {
            Api handler = potentialApi.get();
            if (handler.isAuthorizationRequired() && request.getHeader("Authorization") == null) {
                HttpUtils.forbidden(response);
            }
            else {
                Context context = Context.create(config, jsonValidator, jsonStylesheets);
                Map<String, String> urlParameters = handler.readParametersFromUrl(uri);
                urlParameters.keySet().stream().forEach(k -> context.setVariable(k, urlParameters.get(k)));
                context.setVariable(Context.API_REQUEST,handler.createSource(request));
                context.setVariable(Context.API_REQUEST_BODY, StreamUtils.streamToBytes(request.getInputStream()) );
                boolean result = handler.execute(request, response, context);
                if (result) {
                    HttpResponse responze = (HttpResponse) context.getVariable(Context.API_RESPONSE);
                    HttpUtils.sendResponse(responze, response);
                } else
                    HttpUtils.failure(response);
            }
        }
        else {
            HttpUtils.notFound(response);
        }
    }

}
