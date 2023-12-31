package nl.arba.integration;

import nl.arba.integration.config.Api;
import nl.arba.integration.config.Bean;
import nl.arba.integration.config.Configuration;
import nl.arba.integration.config.Daemon;
import nl.arba.integration.servlets.ApisServlet;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.validation.json.JsonSchema;
import nl.arba.integration.validation.json.JsonValidator;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Projects {
    private final ApisServlet apiServlet;
    private final Daemon[] daemons;
    private final Configuration config;
    private Map<String,Object> jsonStylesheets;

    private JsonValidator validator;

    private Projects(Configuration config, JsonValidator validator, Map<String,Object> jsonstylesheets, Daemon[] daemons) {
        apiServlet = new ApisServlet(config, validator, jsonstylesheets);
        this.daemons = daemons;
        this.config = config;
        this.jsonStylesheets = jsonstylesheets;
        this.validator = validator;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public ApisServlet getApiServlet() {
        return apiServlet;
    }

    public Daemon[] getDaemons() {
        return daemons;
    }

    public static Projects create(File... inputs) throws IOException {
        ArrayList<Api> apis = new ArrayList<>();
        HashMap<String, Map> schemas = new HashMap<>();
        HashMap<String, Object> stylesheets = new HashMap<>();
        stylesheets.put("stylesheets", new HashMap<String,Object>());
        HashMap<String, Object> allsettings = new HashMap<>();
        ArrayList<String> stepClasses = new ArrayList<>();
        ArrayList<Daemon> daemons = new ArrayList<>();
        ArrayList<Bean> beans = new ArrayList<>();

        for (File project: inputs) {
            ZipFile projectzip = new ZipFile(project);
            ZipEntry configEntry = projectzip.getEntry("config.json");
            Configuration config = Configuration.create(projectzip.getInputStream(configEntry));
            String[] validationErrors = config.validate();
            if (validationErrors.length > 0)
                throw new IOException("Invalid configuration (" + Arrays.asList(validationErrors).stream().collect(Collectors.joining("/r/n")) + ")");

            for (String setting: config.getSettings()) {
                if (allsettings.containsKey(setting)) {
                    if (!allsettings.get(setting).equals(config.getSetting(setting)))
                        throw new IOException("Setting '" + setting + "' has multiple values");
                }
                else
                    allsettings.put(setting, config.getSetting(setting));
            }
            for (String stepclass: config.getStepClasses()) {
                if (!stepClasses.contains(stepclass))
                    stepClasses.add(stepclass);
            }
            for (Daemon daemon: config.getDaemons())
                daemons.add(daemon);
            for (Bean bean: config.getBeans()) {
                Optional<Bean> opt = beans.stream().filter(b -> b.getClassname().equals(bean.getClassname()) && b.getName().equals(bean.getName())).findFirst();
                Optional<Bean> problem = beans.stream().filter(b -> !b.getClassname().equals(bean.getClassname()) && b.getName().equals(bean.getName())).findFirst();
                if (problem.isPresent())
                    throw new IOException("Invalid configuration, bean with same name but other classname");
                if (!opt.isPresent())
                    beans.add(bean);
            }

            for (Api api: config.getApis()) {
                Optional<Api> existing = apis.stream().filter(a -> a.getUriPattern().equals(api.getUriPattern())).findFirst();
                if (existing.isPresent()) {
                    throw new IOException("Multiple apis found with same pattern");
                }
                apis.add(api);
            }

            ZipEntry schemaEntry = projectzip.getEntry("jsonschemas.json");
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); InputStream is = projectzip.getInputStream(schemaEntry)) {
                IOUtils.copy(is, bos);
                Map projectschemas = JsonUtils.getMapper().readValue(bos.toByteArray(), Map.class);
                Set<String> schemanames = ((Map) projectschemas.get("schemas")).keySet();
                Optional<String> existing = schemanames.stream().filter(n -> schemas.containsKey(n)).findFirst();
                if (existing.isPresent())
                    throw new IOException("Multiple schema's found with same name");
                else {
                    for (String schema: schemanames)
                        schemas.put(schema, (Map) ((Map) projectschemas.get("schemas")).get(schema));
                }
            }

            ZipEntry stylesheetsEntry = projectzip.getEntry("jsonstylesheets.json");
            Map <String, Object> projectinput = JsonUtils.getMapper().readValue(projectzip.getInputStream(stylesheetsEntry), Map.class);
            final Map <String,Object> projectstylesheets = (Map<String,Object>) projectinput.get("stylesheets");
            Optional<String> existing = projectstylesheets.keySet().stream().filter(k -> stylesheets.containsKey(k)).findFirst();
            if (existing.isPresent()) {
                throw new IOException("Multiple stylesheets found with same name (" + existing.get() + ")");
            }
            for (String key: projectstylesheets.keySet()) {
                ((Map) stylesheets.get("stylesheets")).put(key, projectstylesheets.get(key));
            }
        }

        //Collect configurations
        Configuration config = Configuration.create(new ByteArrayInputStream("{}".getBytes()));
        config.setSettings(allsettings);
        config.setApis(apis.toArray(new Api[0]));
        config.setBeans(beans.toArray(new Bean[0]));
        config.setDaemons(daemons.toArray(new Daemon[0]));
        config.setStepclasses(stepClasses.toArray(new String[0]));

        HashMap<String, Map> inputSchemas = new HashMap<>();
        HashMap<String, Map> allschemas = new HashMap<>();
        schemas.keySet().stream().forEach(s -> allschemas.put(s, schemas.get(s)));
        inputSchemas.put("schemas", allschemas);
        return new Projects(config, JsonValidator.create(JsonSchema.create(new ByteArrayInputStream(JsonUtils.getMapper().writeValueAsString(inputSchemas).getBytes()))), stylesheets, daemons.toArray(new Daemon[0]));
    }

    public Map<String,Object> getJsonStylesheets() {
        return jsonStylesheets;
    }

    public JsonValidator getJsonValidator() {
        return validator;
    }
}
