/*
 * File: PlantUMLLanguageServer.java
 * Author: Norman Babiak
 * Description: LSP server providing diagnostics for PlantUML diagrams
 * Date: 10.4.2026
 */

package com.GLSPPlantUML.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class PlantUMLLanguageServer implements LanguageServer, LanguageClientAware {
    private LanguageClient client;
    private final PlantUMLTextDocumentService textDocumentService;
    private final PlantUMLWorkspaceService workspaceService;
    private int exitCode = 1;

    public PlantUMLLanguageServer() {
        this.textDocumentService = new PlantUMLTextDocumentService(this);
        this.workspaceService = new PlantUMLWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();

        // Sync full document on open/change
        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setSave(new SaveOptions(true));
        capabilities.setTextDocumentSync(syncOptions);
        capabilities.setCompletionProvider(new CompletionOptions());

        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        exitCode = 0;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(exitCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    public LanguageClient getClient() {
        return client;
    }
}