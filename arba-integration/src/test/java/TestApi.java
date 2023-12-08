import nl.arba.integration.App;
import nl.arba.integration.utils.StreamUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestApi {
    /*
    private String token = null;
    private String userId = null;
    */
    private CloseableHttpClient httpClient;
    private File tempFile;

    @Before
    public void before() throws Exception {
        httpClient = HttpClients.createDefault();
        /*
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
        */

        tempFile = File.createTempFile("arba-integration", "config");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
            ZipOutputStream zos = new ZipOutputStream(fos)) {

            InputStream configStream = getClass().getResourceAsStream("/testproject/config.json");
            ZipEntry configEntry = new ZipEntry("config.json");
            zos.putNextEntry(configEntry);
            IOUtils.copy(configStream, zos);
            configStream.close();

            InputStream schemaStream = getClass().getResourceAsStream("/testproject/jsonschemas.json");
            ZipEntry schemaEntry = new ZipEntry("jsonschemas.json");
            zos.putNextEntry(schemaEntry);
            IOUtils.copy(schemaStream, zos);
            schemaStream.close();

            InputStream stylesheetsStream = getClass().getResourceAsStream("/testproject/jsonstylesheets.json");
            ZipEntry stylesheetsEntry = new ZipEntry("jsonstylesheets.json");
            zos.putNextEntry(stylesheetsEntry);
            IOUtils.copy(stylesheetsStream, zos);
            stylesheetsStream.close();
        }
        App.start(8180, new File[] {tempFile}, false);
    }

    /*
    @Test
    public void start_donatie_voorstel() throws Exception {
        //HttpGet get = new HttpGet("http://localhost:8180/ada/ChristenUnie/rootfolders");
        //HttpGet get = new HttpGet("http://192.168.2.74:9602/ada/ChristenUnie/folder/cd435e9f-21a5-11ee-915b-98f2b3f20cf4/subfolders");
        //HttpGet get = new HttpGet("http://localhost:8180/cu/archief/folder/cd435e9f-21a5-11ee-915b-98f2b3f20cf4");
        HttpPost post = new HttpPost("http://localhost:8180/administratie/donatieverzoek/start");
        post.addHeader("Authorization", "Bearer " + token);
        post.addHeader("Content-type", "text/json");
        post.setEntity(new StringEntity("{\"doel\":\"Woord en Daad\",\"bedrag\":1000, \"goedkeurder\":\"" + userId + "\"}"));
        CloseableHttpResponse response = httpClient.execute(post);
        System.out.println(response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void get_tasks() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek");
        get.addHeader("Authorization", "Bearer " + token);
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println(response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void get_external_tasks() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek/beoordeeld");
        get.addHeader("Authorization", "Bearer " + token);
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println(response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void beoordeel_donatie_verzoek() throws Exception {
        HttpPut put = new HttpPut("http://localhost:8180/administratie/donatieverzoek/7c990438-939f-11ee-bf8f-0242ac110004");
        put.addHeader("Authorization", "Bearer " + token);
        put.addHeader("Content-type", "text/json");
        put.setEntity(new StringEntity("{\"doel\":\"Woord en Daad\",\"bedrag\":105, \"goedkeurder\":\"" + userId + "\"}"));
        CloseableHttpResponse response = httpClient.execute(put);
        System.out.println(response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }

    @Test
    public void rondaf_donatie_verzoek() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/administratie/donatieverzoek/960028f3-939f-11ee-bf8f-0242ac110004/rondaf");
        get.addHeader("Authorization", "Bearer " + token);
        get.addHeader("Content-type", "text/json");
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println(response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
    }
    */

    @After
    public void after() {
        tempFile.delete();
        App.stop();
    }

    @Test
    public void testget() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/test");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals("Ongeldige response code", 200, response.getCode());
        String result = StreamUtils.streamToString(response.getEntity().getContent());
        Assert.assertEquals("Ongeldige response", "OK", result);
    }
}
