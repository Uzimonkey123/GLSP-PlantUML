package com.GLSPPlantUML.launcher;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.eclipse.glsp.server.launch.DefaultCLIParser;

public class PlantUMLCLIParser extends DefaultCLIParser {
    public static final String OPTION_WEBSOCKET = "websocket";
    public static final String OPTION_JETTY_LOG_LEVEL = "jettyLogLevel";

    public static final class PlantUMLOptions {
        public static final Level WEBSOCKET_LOG_LEVEL = Level.INFO;
    }

    public PlantUMLCLIParser(final String[] args, final String processName)
            throws ParseException {
        super(args, PlantUMLCLIParser.getDefaultOptions(), processName);
    }

    public boolean isWebsocket() { return hasOption(OPTION_WEBSOCKET); }

    public Level parseWebsocketLogLevel() {
        String levelArg = parseOption(OPTION_JETTY_LOG_LEVEL, PlantUMLOptions.WEBSOCKET_LOG_LEVEL.toString());
        return Level.toLevel(levelArg, PlantUMLOptions.WEBSOCKET_LOG_LEVEL);
    }

    public static Options getDefaultOptions() {
        Options options = DefaultCLIParser.getDefaultOptions();
        options.addOption("w", OPTION_WEBSOCKET, false,
                "Use websocket launcher instead of default launcher.");
        options.addOption("j", OPTION_JETTY_LOG_LEVEL, true,
                "Set the log level for the Jetty websocket server. [default='INFO']");
        return options;
    }

}
