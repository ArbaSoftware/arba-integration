package nl.arba.integration.utils;

import java.util.regex.Pattern;

public class PatternUtils {
    public static String stringLiteral() {
        return "\\'([^\\']*)\\'";
    }

    public static String httpResponseCode() {
        return "(200|401|404|500)";
    }

    public static String uriPart() {
        return "([^\\/]*)";
    }

    public static String apiSourceUriParam() {
        return "\\{api.source\\}\\.uriparams\\.([^\\.]*)";
    }

    public static String apiSourceHeader() { return "\\{api.source\\}\\.headers\\.([^\\.]*)"; }

    public static String createHttpRequest() {
        return "new HttpRequest\\(('get'|'post'),\\'([^\\']*)\\'\\)";
    }

    public static String createCollection() { return "new Collection\\(\\)";}

    public static String createJsonObject() { return "new JsonObject\\(\\)";}
    public static String createJsonArray() { return "new JsonArray\\(\\)";}

    public static String createHttpResponse() {return "new HttpResponse\\(" + PatternUtils.httpResponseCode() + "\\)";}

    public static String parseJson() {return "parseJson(.*)";}

    public static String evalJs() { return "evalJs(.*)";}

    public static String jsonPropertyFilter() { return ".*\\[.*\\]";}

    public static String translateJson() { return "translateJson(.*)";}

    public static void main(String[] args) {
        String test = "{abc}def{ghi}";
        String[] items = test.split("\\{([^\\}]*)\\}");
        System.out.println(Pattern.matches(createHttpRequest(), "new HttpRequest('{ada.base.url}/store/{store}')"));
    }

}
