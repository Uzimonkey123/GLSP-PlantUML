package com.GLSPPlantUML.validators;

import com.diagrams.SequenceDiagram.utils.ErrorRecord;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.error.PSystemError;

import java.util.List;
import java.util.regex.Pattern;

public class ErrorValidator {
    public ErrorRecord checkErrors(String fileText) {
        try {
            SourceStringReader reader = new SourceStringReader(fileText);
            List<BlockUml> blocks = reader.getBlocks();

            for (BlockUml block : blocks) {
                Diagram d = block.getDiagram();
                if (d instanceof PSystemError systemError) {
                    String error = String.join("<br>", systemError.getPureAsciiFormatted());
                    int line = extractLine(systemError);

                    // Get the end of the line to highlight it as whole
                    String[] lines = fileText.split("\n");
                    int colEnd = lines[line].length();

                    return new ErrorRecord(true, error, line, 0, colEnd);
                }
            }

            // No error found
            return new ErrorRecord(false, null, -1, -1, -1);

        }  catch (Exception e) {
            // Fallback if other error failed during parsing
            return new ErrorRecord(true, e.getMessage(), -1, -1, -1);
        }
    }

    private int extractLine(PSystemError errorSystem) {
        Pattern errorPattern = Pattern.compile("line (\\d+)");
        for (String line : errorSystem.getPureAsciiFormatted()) {
            // Find line number by regex, PlantUML format is "Error in line xx..."
            var matcher = errorPattern.matcher(line);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1)) - 1;
            }
        }
        
        return 1;
    }
}
