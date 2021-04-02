package dev.laarryy.Icicle;

import dev.laarryy.Icicle.commands.AddUserToDatabase;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.listeners.Logging;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;


public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);

    public static void main(String[] args) throws Exception {
        // Print token and other args to console
        for (String arg : args) {
            logger.debug(arg);
        }

        if (args.length != 1) {
            logger.error("Invalid Arguments. Allowed Number Of Arguments is 1");
        }

        logger.info("Connecting to Discord!");

        JDABuilder builder = JDABuilder.createDefault(args[0], EnumSet.allOf(GatewayIntent.class));

        JDA api = builder.build();

        logger.debug("Connected! Loading Config");

        // Load Config and Connect to DB

        ConfigManager manager = new ConfigManager();
        manager.loadDatabaseConfig();

        DatabaseLoader loader = new DatabaseLoader();
        loader.openConnection();

        // Add Listeners:

        api.addEventListener(new Logging(api));
        api.addEventListener(new AddUserToDatabase());

    }
}
