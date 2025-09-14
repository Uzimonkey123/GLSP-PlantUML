package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.module.ClassDiagramModule;
import com.GLSPPlantUML.module.SequenceDiagramModule;
import org.apache.commons.cli.ParseException;
//import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
//import org.eclipse.glsp.layout.ElkLayoutEngine;
import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.launch.GLSPServerLauncher;
import org.eclipse.glsp.server.launch.SocketGLSPServerLauncher;
import org.eclipse.glsp.server.utils.LaunchUtil;
import org.eclipse.glsp.server.websocket.WebsocketServerLauncher;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

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
                    new WebsocketServerLauncher(serverModule, "/plantuml",
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
