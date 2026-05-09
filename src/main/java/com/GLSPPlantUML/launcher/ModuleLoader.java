/*
 * File: ModuleLoader.java
 * Author: Norman Babiak
 * Description: Dynamic loader of present diagrams into the server, can be as code, JAR file
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.launcher;

import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.di.ServerModule;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

public class ModuleLoader {
    private final Set<String> loadedDiagramTypes = new HashSet<>();
    private final ServerModule serverModule;
    private final File pluginFolder = new File(
            System.getProperty("user.home"),
            ".glsp-plantuml/plugins"
    );
    public ModuleLoader(ServerModule serverModule) {
        this.serverModule = serverModule;
    }

    /**
     * Main entry point to load modules from either classpath or JAR file
     */
    public void loadModules() throws Exception {
        loadedDiagramTypes.clear();

        loadFolder();
        loadClasspath();
    }

    /**
     * Loads the plugin folders and searches for JAR files
     */
    private void loadFolder() throws IOException {
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        System.err.println("ModuleLoader plugin folder: " + pluginFolder.getAbsolutePath());

        File[] jarFiles = pluginFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jarFiles == null) return;

        for (File jar : jarFiles) {
            loadJar(jar);
        }
    }

    /**
     * Loads JAR files where the class inside is extending DiagramModule from GLSP
     */
    private void loadJar(File jar) throws IOException {
        URL jarUrl = jar.toURI().toURL();

        // Load the diagram module classes with class loader
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, DiagramModule.class.getClassLoader());
        ServiceLoader<DiagramModule> serviceLoader = ServiceLoader.load(DiagramModule.class, loader);

        for (DiagramModule module : serviceLoader) {
            String diagramType = module.getDiagramType();

            if (loadedDiagramTypes.add(diagramType)) {
                serverModule.configureDiagramModule(module);
            }
            else {
                System.err.println("[ModuleLoader] Skipping duplicate diagram type: " + diagramType);
            }
        }
    }

    /**
     * Searches for DiagramModule classes in the overall source code to support in-path diagram implementations too
     */
    private void loadClasspath()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Reflections reflections = new Reflections("com");

        for (Class<? extends DiagramModule> clazz : reflections.getSubTypesOf(DiagramModule.class)) {
            DiagramModule module = clazz.getDeclaredConstructor().newInstance();
            String diagramType = module.getDiagramType();

            if (loadedDiagramTypes.add(diagramType)) {
                serverModule.configureDiagramModule(module);
            }
            else {
                System.err.println("[ModuleLoader] Skipping duplicate diagram type: " + diagramType);
            }
        }
    }
}
