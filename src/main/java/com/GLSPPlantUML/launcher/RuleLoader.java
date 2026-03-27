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

    public void loadRules() throws Exception {
        rulesByDiagram.clear();
        loadedClasses.clear();

        loadFolder();
        loadClasspath();

        System.out.println("RuleLoader: loaded " + loadedClasses.size() + " validation rule(s) for "
                + rulesByDiagram.size() + " diagram type(s)");
    }

    private void loadFolder() throws IOException {
        if (!pluginFolder.isDirectory()) return;

        File[] jars = pluginFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null) return;

        for (File jar : jars) {
            URL jarUrl = jar.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarUrl}, ValidationRule.class.getClassLoader()
            );
            ServiceLoader<ValidationRule> serviceLoader =
                    ServiceLoader.load(ValidationRule.class, loader);

            for (ValidationRule rule : serviceLoader) {
                registerRule(rule);
            }
        }
    }

    private void loadClasspath() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Reflections reflections = new Reflections("com");

        for (Class<? extends ValidationRule> clazz : reflections.getSubTypesOf(ValidationRule.class)) {
            if (loadedClasses.add(clazz)) {
                registerRule(clazz.getDeclaredConstructor().newInstance());
            }
        }
    }

    private void registerRule(ValidationRule rule) {
        rulesByDiagram.computeIfAbsent(rule.getDiagramType(), k -> new ArrayList<>()).add(rule);
    }

    public List<ValidationRule> getRulesForDiagram(String diagramType) {
        return rulesByDiagram.getOrDefault(diagramType, Collections.emptyList());
    }

    public Map<String, List<ValidationRule>> getAllRules() {
        return Collections.unmodifiableMap(rulesByDiagram);
    }
}