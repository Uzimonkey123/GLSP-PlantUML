/*
 * File: PlantUMLParser.java
 * Author: Norman Babiak
 * Description: Interface for the different types of PlantUML diagram parsers
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.parser;

import java.io.File;
import java.io.IOException;

public interface PlantUMLParser<M> {
    M parse(File pumlFile) throws IOException;
}
