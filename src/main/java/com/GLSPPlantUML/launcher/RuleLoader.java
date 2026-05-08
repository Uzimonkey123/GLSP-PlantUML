/*
 * File: RuleLoader.java
 * Author: Norman Babiak
 * Description: Dynamic rule loader and matcher for diagram types
 * Date: 5.5.2026
 */

package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.validators.ValidationRule;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class RuleLoader {
    private final Map<String, List<ValidationRule>> rulesByDiagram = new HashMap<>();
    private final Set<Class<? extends ValidationRule>> loadedClasses = new HashSet<>();
    private final File pluginFolder = new File("plugins");

    /**
     * Main entry point to load modules from either classpath or JAR file
     */
    public void loadRules() throws Exception {
        rulesByDiagram.clear();
        loadedClasses.clear();

        loadFolder();
        loadClasspath();
    }

    /**
     * Loads the plugin folders and searches for JAR files and register the ValidationRule classes
     */
    private void loadFolder() throws IOException {
        if (!pluginFolder.isDirectory()) return;

        File[] jars = pluginFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null) return;

        for (File jar : jars) {
            URL jarUrl = jar.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarUrl}, ValidationRule.class.getClassLoader()
            );
            ServiceLoader<ValidationRule> serviceLoader = ServiceLoader.load(ValidationRule.class, loader);

            for (ValidationRule rule : serviceLoader) {
                registerRule(rule);
            }
        }
    }

    /**
     * Searches for classes implementing ValidationRule in the overall source code to support in-path diagram implementations too
     */
    private void loadClasspath() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Reflections reflections = new Reflections("com");

        for (Class<? extends ValidationRule> clazz : reflections.getSubTypesOf(ValidationRule.class)) {
            if (loadedClasses.add(clazz)) {
                registerRule(clazz.getDeclaredConstructor().newInstance());
            }
        }
    }

    /**
     * Register the rule into the map separated by diagrams
     */
    private void registerRule(ValidationRule rule) {
        rulesByDiagram.computeIfAbsent(rule.getDiagramType(), k -> new ArrayList<>()).add(rule);
    }

    /**
     * Get rules for specific diagram types
     */
    public List<ValidationRule> getRulesForDiagram(String diagramType) {
        return rulesByDiagram.getOrDefault(diagramType, Collections.emptyList());
    }
}