package nl.arba.integration;

import nl.arba.integration.config.Api;
import nl.arba.integration.config.Configuration;
import nl.arba.integration.execution.Daemon;
import nl.arba.integration.execution.steps.AvailableSteps;
import nl.arba.integration.execution.steps.Step;
import nl.arba.integration.servlets.ApisServlet;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.validation.json.JsonSchema;
import nl.arba.integration.validation.json.JsonValidator;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class App {
    private static Server server;
    private static Daemon[] daemons;

    /*
    public static void main(String[] args) {
        try {
            Configuration config = null;
            if (args.length > 0)
                start(new FileInputStream(args[0]));
            else if (System.getProperty("config.resource") != null)
                start(App.class.getResourceAsStream(System.getProperty("config.resource")));
            else
                throw new Exception("No configuration specified");
        }
        catch (Exception err) {
            err.printStackTrace();
        }
    }
     */

    /*
    public static void start(int port, File project) {
        start(port, project, false);
    }
     */

    public static void start(int port, File[] projects, boolean startdaemons) throws IOException {
        try {
            Projects allprojects = Projects.create(projects);

            server = new Server(port);
            ServletHandler handler = new ServletHandler();
            handler.addServletWithMapping(new ServletHolder(allprojects.getApiServlet()), "/*");
            server.setHandler(handler);
            server.start();

            if (startdaemons) {
                nl.arba.integration.config.Daemon[] configuredDaemons = allprojects.getDaemons();
                daemons = new Daemon[configuredDaemons.length];
                int index = 0;
                for (nl.arba.integration.config.Daemon daemon : configuredDaemons) {
                    Daemon newDaemon = new Daemon();
                    newDaemon.setInterval(daemon.getInterval());
                    newDaemon.setSteps(daemon.getSteps());
                    String[] daemonvalidationErrors = newDaemon.validate();
                    if (daemonvalidationErrors.length == 0) {
                        newDaemon.setConfiguration(allprojects.getConfiguration());
                        newDaemon.setJsonStylesheets(allprojects.getJsonStylesheets());
                        newDaemon.setJsonValidator(allprojects.getJsonValidator());
                        newDaemon.start();
                        daemons[index] = newDaemon;
                        index++;
                    } else {
                        System.out.println("Daemon not started because of invalid configuration : " + Arrays.asList(daemonvalidationErrors).stream().collect(Collectors.joining(",")));
                    }
                }
            }

            System.out.println("Integration started at port " + port);
        }
        catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void stop() {
        try {
            server.stop();
            if (daemons != null) {
                for (Daemon daemon : daemons) {
                    if (daemon.isAlive())
                        daemon.halt();
                }
            }
        }
        catch (Exception err) {
            err.printStackTrace();
        }
    }
}