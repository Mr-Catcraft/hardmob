package core.mrcatcraft;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for HardMob.
 * <p>
 * HardMob adds RPG‑style progression to Minecraft mobs:
 * <ul>
 *   <li>Mobs receive a level based on the age of the world (in-game days).</li>
 *   <li>Their attributes (health, damage, speed, knockback resistance) scale with level.</li>
 *   <li>A configurable percentage of mobs become elite bosses with special loot.</li>
 *   <li>Visual customisation includes dynamic size and custom name tags.</li>
 * </ul>
 *
 * @author Mr_Catcraft
 * @version 2.0.0
 */
public class HardMob extends JavaPlugin {

    private static HardMob instance;
    private ConfigManager configManager;
    private MobLevelManager mobLevelManager;
    private EliteMobManager eliteMobManager;
    private NameUpdateTask nameUpdateTask;

    /**
     * Called when the plugin is enabled. Initialises managers, registers listeners,
     * sets the command executor and starts the periodic name-update task.
     */
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        mobLevelManager = new MobLevelManager(this);
        eliteMobManager = new EliteMobManager(this);

        getServer().getPluginManager().registerEvents(new MobModifierListener(this), this);
        getCommand("hardmob").setExecutor(new HardMobCommand(this));

        // Refresh custom names every second to keep health display accurate
        nameUpdateTask = new NameUpdateTask();
        nameUpdateTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("HardMob v2.0.0 enabled");
    }

    /**
     * Called when the plugin is disabled. Cancels the name-update task.
     */
    @Override
    public void onDisable() {
        if (nameUpdateTask != null) {
            nameUpdateTask.cancel();
        }
        getLogger().info("HardMob disabled");
    }

    /**
     * Returns the singleton instance of the plugin.
     *
     * @return the plugin instance
     */
    public static HardMob getInstance() {
        return instance;
    }

    /**
     * @return the configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * @return the mob level manager
     */
    public MobLevelManager getMobLevelManager() {
        return mobLevelManager;
    }

    /**
     * @return the elite mob manager
     */
    public EliteMobManager getEliteMobManager() {
        return eliteMobManager;
    }

    /**
     * Reloads configuration and refreshes all mob names.
     * Intended to be called after editing config.yml and issuing /hardmob reload.
     */
    public void reloadPlugin() {
        reloadConfig();
        configManager.loadConfig();
        mobLevelManager.reload();
        eliteMobManager.reload();
        if (nameUpdateTask != null) {
            nameUpdateTask.updateAllNames();
        }
    }
}