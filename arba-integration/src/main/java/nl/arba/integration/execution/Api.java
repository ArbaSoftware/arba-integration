package nl.arba.integration.execution;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.arba.integration.config.Configuration;
import nl.arba.integration.execution.steps.AvailableSteps;
import nl.arba.integration.execution.steps.Step;
import nl.arba.integration.model.HttpMethod;
import nl.arba.integration.model.HttpRequest;
import nl.arba.integration.utils.PatternUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Api {
    private Pattern uriPattern;
    private String[] uriItems;
    private ArrayList<Step> steps;
    private Configuration config;
    private boolean authRequired = false;
    private List<HttpMethod> methods;
    private nl.arba.integration.config.Api apiConfig;

    private Api(nl.arba.integration.config.Api config, Configuration configuration) {
        apiConfig = config;
        this.config = configuration;
        uriItems = config.getUriPattern().split(Pattern.quote("/"));
        methods = config.getMethods();
        String urlPattern = "";
        for (String item: uriItems) {
            if (!item.isEmpty()) {
                if (item.startsWith("{") && item.endsWith("}")) {
                    urlPattern += "/" + PatternUtils.uriPart();
                }
                else
                    urlPattern += "/" + item;
            }
        }
        uriPattern = Pattern.compile(urlPattern);
        steps = new ArrayList<>();
        for (nl.arba.integration.config.Step step : config.getSteps()) {
            try {
                Step impl = (Step) AvailableSteps.getStep(step.getName()).getConstructor(step.getClass()).newInstance(step);
                steps.add(impl);
            }
            catch (Exception err) {
                err.printStackTrace();
            } //Not possible because of prior validation
        }
        authRequired = config.isAuthorizationRequired();
    }

    public static Api create(nl.arba.integration.config.Api apiconfig, Configuration config) {
        return new Api(apiconfig, config);
    }

    public String getUriPattern() {
        return apiConfig.getUriPattern();
    }

    public boolean handles(String uri) {
        return uriPattern.matcher(uri).matches();
    }

    public HttpRequest createSource(HttpServletRequest request) {
        HttpRequest source = HttpRequest.from(request);
        String[] requestItems = request.getRequestURI().split(Pattern.quote("/"));
        for (int index = 0; index < requestItems.length; index++) {
            if (uriItems[index].startsWith("{") && uriItems[index].endsWith("}")) {
                String uriParam = uriItems[index].substring(1, uriItems[index].length()-1);
                source.setUriParam(uriParam, requestItems[index]);
            }
        }
        return source;
    }

    public boolean execute(HttpServletRequest request, HttpServletResponse response, Context context) {
        boolean succeeded = true;
        for (Step step: this.steps) {
            boolean result = step.execute(context);
            if (!result) {
                succeeded = false;
                System.out.println("Failed at: " + step);
                break;
            }
        }
        return succeeded;
    }

    public boolean isAuthorizationRequired() {
        return authRequired;
    }

    public boolean supports(HttpMethod method) {
        return methods.contains(method);
    }

    public Map<String, String> readParametersFromUrl(String url) {
        String[] urlItems = url.split(Pattern.quote("/"));
        HashMap <String, String> result = new HashMap<>();
        for (int index = 0; index < uriItems.length; index++) {
            String item = uriItems[index];
            if (Pattern.matches(Pattern.quote("{") + ".*" + Pattern.quote("}"), item)) {
                result.put(item.substring(1, item.length()-1), urlItems[index]);
            }
        }
        return result;
    }
}
