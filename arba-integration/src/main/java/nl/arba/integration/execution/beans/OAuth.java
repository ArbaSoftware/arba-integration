package nl.arba.integration.execution.beans;

import nl.arba.integration.execution.Context;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class OAuth {
    private Context context;
    private static HashMap<String,String> tokenCache = new HashMap<>();
    private static HashMap<String,String> refreshtokenCache = new HashMap<>();

    public OAuth(Context context) {
        this.context = context;
    }

    public String getToken(String username, String password) throws Exception {
        return getToken(username, password, true);
    }

    private String getToken(String username, String password, boolean cached) throws Exception {
        System.out.println("OAuth.getToken: " + username + "/"+ password);
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
            Map jsonResponse = JsonUtils.getMapper().readValue(response.getEntity().getContent(), Map.class);
            if (jsonResponse.containsKey("active") && jsonResponse.get("active").equals(Boolean.FALSE)) {
                //Refresh token
                System.out.println("Refreshing token");
                post = new HttpPost((String) context.getConfiguration().getSetting("oauth.token.url"));
                request = new ArrayList<>();
                request.add(new BasicNameValuePair("client_secret", (String) context.getConfiguration().getSetting("oauth.client.secret")));
                request.add(new BasicNameValuePair("client_id", (String) context.getConfiguration().getSetting("oauth.client.id")));
                request.add(new BasicNameValuePair("grant_type", "refresh_token"));
                request.add(new BasicNameValuePair("refresh_token", refreshtokenCache.get(cacheKey)));
                post.setEntity(new UrlEncodedFormEntity(request));
                CloseableHttpResponse refreshresponse = client.execute(post);
                jsonResponse = JsonUtils.getMapper().readValue(refreshresponse.getEntity().getContent(), Map.class);
                token = jsonResponse.get("access_token").toString();
                refreshtokenCache.put(cacheKey, jsonResponse.get("refresh_token").toString());
                tokenCache.put(cacheKey, token);
            }
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
            Map jsonResponse = JsonUtils.getMapper().readValue(response.getEntity().getContent(), Map.class);
            String token = (String) jsonResponse.get("access_token");
            if (cached) {
                tokenCache.put(cacheKey, token);
                refreshtokenCache.put(cacheKey, jsonResponse.get("refresh_token").toString());
            }
            return token;
        }
    }

    public String getTokenHeader(String username, String password) throws Exception {
        return "Bearer " + getToken(username, password, false);
    }

    public String getUserFromToken(String token) throws Exception {
        Map tokenInfo = JsonUtils.getMapper().readValue(Base64.decodeBase64(token.split(Pattern.quote("."))[1]), Map.class);
        return (String) tokenInfo.get("sub");
    }
}
