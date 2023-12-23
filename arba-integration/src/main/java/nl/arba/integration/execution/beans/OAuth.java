package nl.arba.integration.execution.beans;

import nl.arba.integration.execution.Context;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OAuth {
    private Context context;
    private static HashMap<String,String> tokenCache = new HashMap<>();

    public OAuth(Context context) {
        this.context = context;
    }

    public String getToken(String username, String password) throws Exception {
        return getToken(username, password, true);
    }

    private String getToken(String username, String password, boolean cached) throws Exception {
        String cacheKey = username + ":"+ password;
        CloseableHttpClient client = HttpClients.createDefault();
        if (cached && tokenCache.containsKey(cacheKey)) {
            //Validate token
            HttpPost post = new HttpPost((String) context.getConfiguration().getSetting("oauth.validation.url"));
            String token = tokenCache.get(cacheKey);
            ArrayList<NameValuePair> request = new ArrayList<>();
            request.add(new BasicNameValuePair("token", token));
            request.add(new BasicNameValuePair("username", username));
            request.add(new BasicNameValuePair("client_secret", (String) context.getConfiguration().getSetting("oauth.client.secret")));
            request.add(new BasicNameValuePair("client_id", (String) context.getConfiguration().getSetting("oauth.client.id")));
            post.setEntity(new UrlEncodedFormEntity(request));
            CloseableHttpResponse response = client.execute(post);
            return token;
        }
        else {
            HttpPost post = new HttpPost((String) context.getConfiguration().getSetting("oauth.token.url"));
            ArrayList<NameValuePair> request = new ArrayList<>();
            request.add(new BasicNameValuePair("client_secret", (String) context.getConfiguration().getSetting("oauth.client.secret")));
            request.add(new BasicNameValuePair("client_id", (String) context.getConfiguration().getSetting("oauth.client.id")));
            request.add(new BasicNameValuePair("grant_type", "password"));
            request.add(new BasicNameValuePair("username", username));
            request.add(new BasicNameValuePair("password", password));
            post.setEntity(new UrlEncodedFormEntity(request));
            CloseableHttpResponse response = client.execute(post);
            String token = (String) JsonUtils.getMapper().readValue(response.getEntity().getContent(), Map.class).get("access_token");
            if (cached)
                tokenCache.put(cacheKey, token);
            return token;
        }
    }

    public String getTokenHeader(String username, String password) throws Exception {
        return "Bearer " + getToken(username, password, false);
    }
}
