import nl.arba.integration.config.Configuration;
import nl.arba.integration.execution.steps.AvailableSteps;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TestConfiguration {
    @Test
    public void test() throws IOException {
        AvailableSteps.getAvailableSteps();
        Configuration config = Configuration.create(getClass().getResourceAsStream("/testproject/config.json"));
        String[] configurationErrors = config.validate();
        Assert.assertEquals("Configuratie is ongeldig ( " + (Arrays.asList(configurationErrors)).stream().collect(Collectors.joining("\r\n")) + ")", 0, configurationErrors.length);
    }
}
