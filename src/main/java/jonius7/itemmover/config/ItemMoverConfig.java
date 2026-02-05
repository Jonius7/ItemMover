package jonius7.itemmover.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ItemMoverConfig {

    public static Set<String> defaultPullBlacklist = new HashSet<>(Arrays.asList(
        "thaumcraft:blockTable:15",
        "tconstruct:toolstationblock"
    ));

    public static Set<String> pullBlacklist = new HashSet<>();
    public static Set<String> pushBlacklist = new HashSet<>();

    private static Configuration config;

    public static void load(File file) {
        config = new Configuration(file);
        config.load();

        // --- Pull blacklist ---
        String[] defaultPull = defaultPullBlacklist.toArray(new String[0]);
        String[] pullFromConfig = config.getStringList(
            "pullBlacklist", "general", defaultPull, "Blocks that cannot be pulled"
        );
        pullBlacklist = new HashSet<>();
        for (String s : pullFromConfig) {
            pullBlacklist.add(s.toLowerCase());
        }

        // --- Push blacklist ---
        String[] defaultPush = new String[0];
        String[] pushFromConfig = config.getStringList(
            "pushBlacklist", "general", defaultPush, "Blocks that cannot be pushed"
        );
        pushBlacklist = new HashSet<>();
        for (String s : pushFromConfig) {
            pushBlacklist.add(s.toLowerCase());
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean isPullBlacklisted(String blockName) {
        return pullBlacklist.contains(blockName.toLowerCase());
    }

    public static boolean isPushBlacklisted(String blockName) {
        return pushBlacklist.contains(blockName.toLowerCase());
    }
}