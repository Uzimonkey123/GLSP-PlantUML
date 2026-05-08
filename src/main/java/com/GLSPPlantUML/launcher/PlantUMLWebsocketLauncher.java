/*
 * File: PlantUMLWebsocketLauncher.java
 * Author: Norman Babiak
 * Description: Custom Websocket launcher for server, just so it is possible to set the idle timeout
 * Date: 5.5.2026
 */

package com.GLSPPlantUML.launcher;

import org.apache.logging.log4j.Level;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.websocket.GLSPConfigurator;
import org.eclipse.glsp.server.websocket.GLSPServerEndpoint;
import org.eclipse.glsp.server.websocket.WebsocketServerLauncher;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;

import jakarta.websocket.server.ServerEndpointConfig;
import java.net.InetSocketAddress;

public class PlantUMLWebsocketLauncher extends WebsocketServerLauncher {
    private static final long IDLE_TIMEOUT_MS = 10 * 60 * 1000;

    public PlantUMLWebsocketLauncher(ServerModule serverModule, String endpointPath, Level websocketLogLevel) {
        super(serverModule, endpointPath, websocketLogLevel);
    }

    /**
     * Base start method for the websocket, just to extend the idle timeout
     */
    @Override
    public void start(String hostname, int port) {
        try {
            this.server = new Server(new InetSocketAddress(hostname, port));
            ServletContextHandler webAppContext = new ServletContextHandler(1);
            webAppContext.setContextPath("/");

            JakartaWebSocketServletContainerInitializer.configure(webAppContext, (servletContext, wsContainer) -> {
                wsContainer.setDefaultMaxSessionIdleTimeout(IDLE_TIMEOUT_MS);

                ServerEndpointConfig.Builder builder = ServerEndpointConfig.Builder
                        .create(GLSPServerEndpoint.class, "/" + this.endpointPath);
                builder.configurator(new GLSPConfigurator(this::createInjector));
                wsContainer.addEndpoint(builder.build());
            });

            this.server.setHandler(webAppContext);
            this.server.start();

            int actualPort = port == 0 ? this.server.getURI().getPort() : port;
            LOGGER.info("GLSP server running on: {}{}", this.server.getURI(), this.endpointPath);
            System.out.println(getStartupCompleteMessage() + actualPort);

            this.server.join();
        } catch (Exception ex) {
            LOGGER.error("Failed to start Websocket GLSP server: {}", ex.getMessage(), ex);
        }
    }
}