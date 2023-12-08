package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;

import java.io.InputStream;
import java.util.Map;

public class TranslateJson extends Step {
    public TranslateJson(nl.arba.integration.config.Step config) {
        super(config);
    }

    @Override
    public boolean execute(Context context) {
        Object source = context.evaluate(getConfiguration().getSetting("source").toString());
        InputStream streamSource = StreamUtils.objectToStream(source);
        Map<String,Object> allstylesheets = context.getJsonStylesheets();
        context.setVariable(getConfiguration().getSetting("outputvariable").toString(), JsonUtils.translate(streamSource, allstylesheets, getConfiguration().getSetting("stylesheet").toString()));
        return true;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {
                "source",
                "stylesheet",
                "outputvariable"
        };
    }
}
