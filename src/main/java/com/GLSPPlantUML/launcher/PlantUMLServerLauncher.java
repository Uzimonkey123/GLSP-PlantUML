/*
 * File: PlantUMLServerLauncher.java
 * Author: Norman Babiak
 * Description: Main launcher for the servers and processes
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.validators.CompositeValidator;
import com.GLSPPlantUML.validators.ErrorValidator;
import org.apache.commons.cli.ParseException;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.launch.GLSPServerLauncher;
import org.eclipse.glsp.server.launch.SocketGLSPServerLauncher;
import org.eclipse.glsp.server.utils.LaunchUtil;
import org.eclipse.glsp.server.websocket.WebsocketServerLauncher;

public class PlantUMLServerLauncher {
    public static void main(String[] args) {
        String processName = "GLSPPlantUML-1.0-SNAPSHOT.jar";
        ValidationServer httpServer = validationStartup();

        try {
            PlantUMLCLIParser parser = new PlantUMLCLIParser(args, processName);

            int port = parser.parsePort();
            String host = parser.parseHostname();

            ServerModule serverModule = new GLSPServerModule();
            ModuleLoader moduleLoader = new ModuleLoader(serverModule);
            moduleLoader.loadModules();

            GLSPServerLauncher launcher = parser.isWebsocket() ?
                    new PlantUMLWebsocketLauncher(serverModule, "/plantuml",
                    parser.parseWebsocketLogLevel()) :
                    new SocketGLSPServerLauncher(serverModule);

            launcher.start(host, port, parser);

        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println();
            LaunchUtil.printHelp(processName, PlantUMLCLIParser.getDefaultOptions());

        } catch (NoSuchMethodException e) { // TODO error handling properly
            throw new RuntimeException(e);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            if (httpServer != null) {
                httpServer.stop();
            }
        }
    }

    private static ValidationServer validationStartup() {
        try {
            RuleLoader ruleLoader = new RuleLoader();
            ruleLoader.loadRules();

            ErrorValidator errorValidator = new ErrorValidator();
            CompositeValidator compositeValidator = new CompositeValidator(errorValidator, ruleLoader);

            // Start HTTP validation server
            ValidationServer server = new ValidationServer(compositeValidator);
            server.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
                System.out.println("Validation HTTP server stopped");
            }));

            return server;

        } catch (Exception e) {
            System.err.println("Failed to start validation HTTP server: " + e.getMessage());
            e.printStackTrace();

            return null;
        }
    }
}