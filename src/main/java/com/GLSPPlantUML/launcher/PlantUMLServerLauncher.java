package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.module.SequenceDiagramModule;
import org.apache.commons.cli.ParseException;
//import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
//import org.eclipse.glsp.layout.ElkLayoutEngine;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.launch.GLSPServerLauncher;
import org.eclipse.glsp.server.launch.SocketGLSPServerLauncher;
import org.eclipse.glsp.server.utils.LaunchUtil;
import org.eclipse.glsp.server.websocket.WebsocketServerLauncher;

public class PlantUMLServerLauncher {
    public static void main(String[] args) {
        String processName = "GLSPPlantUML-1.0-SNAPSHOT.jar";
        try {
            PlantUMLCLIParser parser = new PlantUMLCLIParser(args, processName);
            //TODO: Add elk layout engine

            int port = parser.parsePort();
            String host = parser.parseHostname();
            ServerModule serverModule = new GLSPServerModule()
                    .configureDiagramModule(new SequenceDiagramModule());

            GLSPServerLauncher launcher = parser.isWebsocket() ?
                    new WebsocketServerLauncher(serverModule, "/plantuml",
                    parser.parseWebsocketLogLevel()) :
                    new SocketGLSPServerLauncher(serverModule);

            launcher.start(host, port, parser);
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println();
            LaunchUtil.printHelp(processName, PlantUMLCLIParser.getDefaultOptions());
        }
    }
}
