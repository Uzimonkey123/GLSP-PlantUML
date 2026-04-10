/*
 * File: LSPServerLauncher.java
 * Author: Norman Babiak
 * Description: Launches the PlantUML LSP server over stdin/stdout
 * Date: 10.4.2026
 */

package com.GLSPPlantUML.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Future;

public class LSPServerLauncher {

    public static void main(String[] args) {
        try {
            // Setting output, so it does not mix for the client side
            PrintStream originalOut = System.out;
            System.setOut(System.err);

            startServer(System.in, originalOut);

        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public static void startServer(InputStream in, OutputStream out) {
        PlantUMLLanguageServer server = new PlantUMLLanguageServer();

        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
        Future<?> listening = launcher.startListening();

        server.connect(launcher.getRemoteProxy());

        try {
            listening.get();

        } catch (Exception e) {
            System.err.println("LSP server terminated: " + e.getMessage());
        }
    }
}