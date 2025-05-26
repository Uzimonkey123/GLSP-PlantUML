package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.PlantUMLGLSPServer;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.protocol.GLSPServer;

public class GLSPServerModule extends ServerModule {

    @Override
    protected Class<? extends GLSPServer> bindGLSPServer() {
        return PlantUMLGLSPServer.class;
    }
}
