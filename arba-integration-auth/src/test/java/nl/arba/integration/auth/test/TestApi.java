package nl.arba.integration.auth.test;

import nl.arba.integration.App;
import nl.arba.integration.utils.StreamUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestApi {
    private File tempFile;
    private CloseableHttpClient httpClient;

    @Before
    public void before() throws Exception {
        httpClient = (CloseableHttpClient) HttpClients.createDefault();
        tempFile = File.createTempFile("arba-integration-auth", "config");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            InputStream configStream = new FileInputStream("./src/main/config.json");
            ZipEntry configEntry = new ZipEntry("config.json");
            zos.putNextEntry(configEntry);
            IOUtils.copy(configStream, zos);
            configStream.close();

            InputStream schemaStream = new FileInputStream("./src/main/jsonschemas.json");
            ZipEntry schemaEntry = new ZipEntry("jsonschemas.json");
            zos.putNextEntry(schemaEntry);
            IOUtils.copy(schemaStream, zos);
            schemaStream.close();

            InputStream stylesheetsStream = new FileInputStream("./src/main/jsonstylesheets.json");
            ZipEntry stylesheetsEntry = new ZipEntry("jsonstylesheets.json");
            zos.putNextEntry(stylesheetsEntry);
            IOUtils.copy(stylesheetsStream, zos);
            stylesheetsStream.close();
        }
        catch (Exception err) {
            err.printStackTrace();
            throw err;
        }
        App.start(8180, new File[] {tempFile}, false);
    }

    @After
    public void after() {
        App.stop();
        tempFile.delete();
    }

    @Test
    public void test_users() throws Exception {
        HttpGet get = new HttpGet("http://localhost:8180/auth/users");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals("Ongeldige response code (" + response.getCode() + ")", 200, response.getCode());
        System.out.println(StreamUtils.streamToString(response.getEntity().getContent()));
        //Assert.assertEquals("Ongeldige response", "Got users", StreamUtils.streamToString(response.getEntity().getContent()));
    }
}
