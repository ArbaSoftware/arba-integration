package nl.arba.integration.model;

public enum HttpMethod {
    GET,POST;

    public static HttpMethod fromString(String value) {
        if ("get".equalsIgnoreCase(value))
            return GET;
        else
            return POST;
    }
}
