/*
 * File: PlantUMLServerLauncher.java
 * Author: Norman Babiak
 * Description: Main launcher for the servers and processes
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.launcher;

import org.apache.commons.cli.ParseException;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.launch.GLSPServerLauncher;
import org.eclipse.glsp.server.launch.SocketGLSPServerLauncher;
import org.eclipse.glsp.server.utils.LaunchUtil;

public class PlantUMLServerLauncher {
    public static void main(String[] args) {
        String processName = "GLSPPlantUML-1.0-SNAPSHOT.jar";

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
        }
    }
}