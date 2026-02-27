package com.GLSPPlantUML.launcher;

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
        ValidationServer server = null;

        // Create validation service and HTTP server
        ErrorValidator validationService = new ErrorValidator();
        server = new ValidationServer(validationService);

        try {
            server.start();

        } catch (Exception e) { // TODO error handling properly
            System.err.println("Failed to start validation HTTP server: " + e.getMessage());
            e.printStackTrace();

            return null;
        }

        // Add shutdown hook for HTTP server
        final ValidationServer finalHttpServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            finalHttpServer.stop();
            System.out.println("Validation HTTP server stopped");
        }));

        return server;
    }
}