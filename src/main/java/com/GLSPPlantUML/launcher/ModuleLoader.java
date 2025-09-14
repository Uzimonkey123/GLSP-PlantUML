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
    private final Set<Class<? extends DiagramModule>> loadedModuleClasses = new HashSet<>();
    private final ServerModule serverModule;
    private final File pluginFolder = new File("plugins");

    public ModuleLoader(ServerModule serverModule) {
        this.serverModule = serverModule;
    }

    public void loadModules() throws Exception {
        loadedModuleClasses.clear();

        loadFolder();
        loadClasspath();
    }

    private void loadFolder() throws IOException {
        if (!pluginFolder.isDirectory()) return;

        File[] jarFiles = pluginFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jarFiles == null) return;

        for (File jar : jarFiles) {
            loadJar(jar);
        }
    }

    private void loadJar(File jar) throws IOException {
        URL jarUrl = jar.toURI().toURL();

        // Load the diagram module classes with class loader
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, DiagramModule.class.getClassLoader());
        ServiceLoader<DiagramModule> serviceLoader = ServiceLoader.load(DiagramModule.class, loader);

        for (DiagramModule module : serviceLoader) {
            Class<? extends DiagramModule> clazz = module.getClass();
            if (loadedModuleClasses.add(clazz)) {
                serverModule.configureDiagramModule(module);
            }
        }
    }

    private void loadClasspath()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Reflections reflections = new Reflections("com");

        for (Class<? extends DiagramModule> clazz : reflections.getSubTypesOf(DiagramModule.class)) {
            if (loadedModuleClasses.add(clazz)) {
                serverModule.configureDiagramModule(clazz.getDeclaredConstructor().newInstance());
            }
        }
    }
}
