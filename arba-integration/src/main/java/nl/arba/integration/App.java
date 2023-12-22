package nl.arba.integration;

import nl.arba.integration.execution.Daemon;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class App {
    private static Server server;
    private static Daemon[] daemons;

    public static void main(String[] args) {
        try {
            File[] projects = new File[args.length-1];
            for (int index = 1; index < args.length; index++) {
                projects[index - 1] = new File(args[index]);
                if (!projects[index-1].exists())
                    System.out.println(projects[index-1] + " not found!");
            }
            App.start(Integer.parseInt(args[0]), projects, true);
        }
        catch (Exception err) {
            err.printStackTrace();
        }
    }

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
                    String[] daemonvalidationErrors = newDaemon.validate(allprojects.getConfiguration().getStepClasses());
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