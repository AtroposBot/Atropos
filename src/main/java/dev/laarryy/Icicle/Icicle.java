package dev.laarryy.Icicle;

import dev.laarryy.Icicle.config.ConfigLoader;
import dev.laarryy.Icicle.config.ConfigSettings;
import dev.laarryy.Icicle.listeners.Logging;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;
import java.util.EnumSet;


public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);

    public static void main(String[] args) throws Exception {
        // Print token and other args to console
        for (String arg : args) { logger.debug(arg); }

        if (args.length != 1) {
            logger.error("Invalid Arguments. Allowed Number Of Arguments is 1");
        }

        logger.info("Connecting to Discord!");

        JDABuilder builder = JDABuilder.createDefault(args[0], EnumSet.allOf(GatewayIntent.class));

        JDA api = builder.build();

        logger.debug("Connected! Loading Config");

        // Load Config and Connect to DB

        DatabaseLoader loader = new DatabaseLoader();
        try {
            loader.loadDatabaseConfig();
            loader.openConnection();
        } catch (ConfigurateException e) {
            logger.error("Error loading database config: " + e.getMessage());
            System.exit(1);
        }

        // Add Listeners:

        api.addEventListener(new Logging(api));


    }

}
