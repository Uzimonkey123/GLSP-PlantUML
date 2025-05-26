package com.GLSPPlantUML.parser;

import java.io.File;
import java.io.IOException;

public interface PlantUMLParser<M> {
    M parse(File pumlFile) throws IOException;
}
