import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arba.integration.App;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestExtern {
    private CloseableHttpClient httpClient;
    private File tempFile;
    private String token;
    private String userId;

    @Before
    public void before() throws Exception {
        httpClient = HttpClients.createDefault();
        //Get token
        String tokenUrl = "http://192.168.2.74:9443/auth/realms/Arba/protocol/openid-connect/token";
        HttpPost post = new HttpPost(tokenUrl);
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", "administratie"));
        params.add(new BasicNameValuePair("client_secret", "D41RLqhN1xnHPaNY8VsLzAYIx9L4JlMa"));
        params.add(new BasicNameValuePair("username", "arjan"));
        params.add(new BasicNameValuePair("password", "hemertje"));
        params.add(new BasicNameValuePair("grant_type", "password"));
        post.setEntity(new UrlEncodedFormEntity(params));
        CloseableHttpResponse response = httpClient.execute(post);

        try (InputStream is = response.getEntity().getContent()) {
            ObjectMapper mapper = new ObjectMapper();
            Map tokenInfo = mapper.readValue(is, Map.class);
            token = tokenInfo.get("access_token").toString();
            Map tokenItems = new ObjectMapper().readValue(Base64.getDecoder().decode(token.split(Pattern.quote("."))[1]), Map.class);
            userId = tokenItems.get("sub").toString();
        }
        catch (Exception err) {
            err.printStackTrace();
        }

        String configdir = "/home/arjan/dev/src/administratie/integration/";
        tempFile = File.createTempFile("arba-integration", "config");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            InputStream configStream = new FileInputStream(configdir + "/config.json");
            ZipEntry configEntry = new ZipEntry("config.json");
            zos.putNextEntry(configEntry);
            IOUtils.copy(configStream, zos);
            configStream.close();

            InputStream schemaStream = new FileInputStream(configdir + "/jsonschemas.json");
            ZipEntry schemaEntry = new ZipEntry("jsonschemas.json");
            zos.putNextEntry(schemaEntry);
            IOUtils.copy(schemaStream, zos);
            schemaStream.close();

            InputStream stylesheetsStream = new FileInputStream(configdir + "/jsonstylesheets.json");
            ZipEntry stylesheetsEntry = new ZipEntry("jsonstylesheets.json");
            zos.putNextEntry(stylesheetsEntry);
            IOUtils.copy(stylesheetsStream, zos);
            stylesheetsStream.close();
        }
        catch (Exception err) {
            err.printStackTrace();
        }
        App.start(8180, new File[] {tempFile}, false);
    }

    @Test
    public void test_start() throws Exception {
        HttpPost post = new HttpPost("http://localhost:8180/administratie/donatieverzoek/start");
        post.addHeader("Authorization", "Bearer " + token);
        HashMap <String, Object> jsonInput = new HashMap<>();
        jsonInput.put("doel", "Woord en daad");
        jsonInput.put("bedrag", 125);
        jsonInput.put("goedkeurder", "abc");
        post.setEntity(new ByteArrayEntity(JsonUtils.getMapper().writeValueAsBytes(jsonInput), ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = httpClient.execute(post);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void test_list() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek");
        get.addHeader("Authorization", "Bearer " + token);
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void test_beoordeelde_taken() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek/beoordeeld");
        get.addHeader("Authorization", "Bearer " + token);
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void test_all() throws Exception {
        HttpPost post = new HttpPost("http://localhost:8180/administratie/donatieverzoek/start");
        post.addHeader("Authorization", "Bearer " + token);
        HashMap <String, Object> jsonInput = new HashMap<>();
        jsonInput.put("doel", "Woord en daad");
        jsonInput.put("bedrag", 125);
        jsonInput.put("goedkeurder", "abc");
        post.setEntity(new ByteArrayEntity(JsonUtils.getMapper().writeValueAsBytes(jsonInput), ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = httpClient.execute(post);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());

        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek");
        get.addHeader("Authorization", "Bearer " + token);
        response = httpClient.execute(get);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
        Map[] items = JsonUtils.getMapper().readValue(response.getEntity().getContent(), Map[].class);

        String itemId = items[0].get("id").toString();

        HttpPut put = new HttpPut("http://localhost:8180/administratie/donatieverzoek/" + itemId);
        put.addHeader("Authorization", "Bearer "+ token);
        HashMap<String,Object> request = new HashMap<>();
        request.put("doel", "Woord en Daad");
        request.put("bedrag", 126);
        request.put("akoord", true);
        put.setEntity(new ByteArrayEntity(JsonUtils.getMapper().writeValueAsBytes(request), ContentType.APPLICATION_JSON));
        response = httpClient.execute(put);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
    }

    @After
    public void after() {
        tempFile.delete();
        App.stop();
    }
}
