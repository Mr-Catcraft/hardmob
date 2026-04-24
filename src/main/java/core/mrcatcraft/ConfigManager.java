package core.mrcatcraft;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Responsible for reading and caching all values from config.yml.
 * Provides type‑safe getters for every configuration option.
 */
public class ConfigManager {

    private final HardMob plugin;

    // General
    private boolean enabled;
    private boolean levelEnabled;

    // Level formula
    private double formulaA;
    private double formulaB;
    private double formulaC;

    // Distribution
    private String distribution;

    // Scaling
    private double healthPercentPerLevel;
    private double damagePercentPerLevel;
    private double speedPercentPerLevel;
    private double knockbackResistPerLevel;

    // Equipment
    private Map<Integer, String> equipmentThresholds;
    private double enchantMultiplier;

    // Potion effects
    private double potionChancePerLevel;
    private double potionDurationMultiplier;
    private int potionAmplifierBase;
    private int potionMaxDurationSeconds;

    // Elite mobs
    private boolean eliteEnabled;
    private double eliteSpawnChancePercent;
    private double eliteHealthMultiplier;
    private double eliteDamageMultiplier;
    private double eliteSpeedMultiplier;
    private double eliteSize;
    private boolean eliteAlwaysEquipped;
    private int eliteDiamondsMin;
    private int eliteDiamondsMax;
    private int eliteGoldMin;
    private int eliteGoldMax;
    private List<String> eliteMusicDiscs;
    private int eliteExpPerLevel;

    // Visual
    private boolean dynamicSizeEnabled;
    private double dynamicSizeMin;
    private double dynamicSizeMax;
    private boolean customNameEnabled;
    private boolean customNameVisible;
    private String customNameFormat;
    private String customNameBossFormat;
    private ChatColor colorLow;
    private ChatColor colorMedium;
    private ChatColor colorHigh;
    private ChatColor colorBoss;

    // Restrictions
    private Set<String> worldBlacklist;
    private Set<String> mobWhitelist;

    // Debug
    private boolean debugMode;

    /**
     * Constructs the config manager and immediately loads values.
     *
     * @param plugin the main plugin instance
     */
    public ConfigManager(HardMob plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Reads (or rereads) every value from config.yml into cached fields.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("general.enabled", true);
        levelEnabled = config.getBoolean("level_system.enabled", true);

        formulaA = config.getDouble("level_system.formula.A", 99.0);
        formulaB = config.getDouble("level_system.formula.B", 98.0);
        formulaC = config.getDouble("level_system.formula.C", 40.0);
        distribution = config.getString("level_system.distribution", "uniform");

        healthPercentPerLevel = config.getDouble("level_system.scaling.health_percent_per_level", 2.0);
        damagePercentPerLevel = config.getDouble("level_system.scaling.damage_percent_per_level", 1.5);
        speedPercentPerLevel = config.getDouble("level_system.scaling.speed_percent_per_level", 0.3);
        knockbackResistPerLevel = config.getDouble("level_system.scaling.knockback_resist_per_level", 0.01);

        // Equipment thresholds
        equipmentThresholds = new LinkedHashMap<>();
        if (config.contains("level_system.equipment.thresholds")) {
            config.getConfigurationSection("level_system.equipment.thresholds")
                    .getValues(false)
                    .forEach((key, value) -> equipmentThresholds.put(Integer.parseInt(key), value.toString()));
        }
        enchantMultiplier = config.getDouble("level_system.equipment.enchant_multiplier", 0.2);

        potionChancePerLevel = config.getDouble("level_system.potion_effects.chance_per_level", 0.5);
        potionDurationMultiplier = config.getDouble("level_system.potion_effects.duration_multiplier", 1.0);
        potionAmplifierBase = config.getInt("level_system.potion_effects.amplifier_base", 0);
        potionMaxDurationSeconds = config.getInt("level_system.potion_effects.max_duration_seconds", 600);

        // Elite mobs
        eliteEnabled = config.getBoolean("elite_mobs.enabled", true);
        eliteSpawnChancePercent = config.getDouble("elite_mobs.spawn_chance_percent", 0.5);
        eliteHealthMultiplier = config.getDouble("elite_mobs.health_multiplier", 2.0);
        eliteDamageMultiplier = config.getDouble("elite_mobs.damage_multiplier", 2.0);
        eliteSpeedMultiplier = config.getDouble("elite_mobs.speed_multiplier", 0.7);
        eliteSize = config.getDouble("elite_mobs.size", 2.0);
        eliteAlwaysEquipped = config.getBoolean("elite_mobs.always_equipped", true);

        eliteDiamondsMin = config.getInt("elite_mobs.drops.diamonds.min", 0);
        eliteDiamondsMax = config.getInt("elite_mobs.drops.diamonds.max", 4);
        eliteGoldMin = config.getInt("elite_mobs.drops.gold.min", 0);
        eliteGoldMax = config.getInt("elite_mobs.drops.gold.max", 20);
        eliteMusicDiscs = config.getStringList("elite_mobs.drops.music_discs");
        eliteExpPerLevel = config.getInt("elite_mobs.drops.exp_per_level", 10);

        // Visual
        dynamicSizeEnabled = config.getBoolean("visual.dynamic_size.enabled", true);
        dynamicSizeMin = config.getDouble("visual.dynamic_size.min", 0.8);
        dynamicSizeMax = config.getDouble("visual.dynamic_size.max", 1.2);
        customNameEnabled = config.getBoolean("visual.custom_name.enabled", true);
        customNameVisible = config.getBoolean("visual.custom_name.visible", true);
        customNameFormat = config.getString("visual.custom_name.format",
                "&8[&c%health%&8/&c%max_health%&8] &f%name% &8[&6%level% lvl&8]");
        customNameBossFormat = config.getString("visual.custom_name.format_boss",
                "&8[&c%health%&8/&c%max_health%&8] &f%name% &8[&4BOSS&8]");

        colorLow = parseColor(config.getString("visual.custom_name.colors.low_level", "GREEN"));
        colorMedium = parseColor(config.getString("visual.custom_name.colors.medium_level", "YELLOW"));
        colorHigh = parseColor(config.getString("visual.custom_name.colors.high_level", "RED"));
        colorBoss = parseColor(config.getString("visual.custom_name.colors.boss", "DARK_RED"));

        worldBlacklist = Set.copyOf(config.getStringList("world_blacklist"));
        mobWhitelist = Set.copyOf(config.getStringList("mob_whitelist"));

        debugMode = config.getBoolean("debug_mode", false);
    }

    /**
     * Converts a string to a {@link ChatColor}, defaults to WHITE on failure.
     */
    private ChatColor parseColor(String name) {
        try {
            return ChatColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }

    // -------------------- Getters --------------------

    public boolean isEnabled() { return enabled; }
    public boolean isLevelEnabled() { return levelEnabled; }
    public double getFormulaA() { return formulaA; }
    public double getFormulaB() { return formulaB; }
    public double getFormulaC() { return formulaC; }
    public String getDistribution() { return distribution; }
    public double getHealthPercentPerLevel() { return healthPercentPerLevel; }
    public double getDamagePercentPerLevel() { return damagePercentPerLevel; }
    public double getSpeedPercentPerLevel() { return speedPercentPerLevel; }
    public double getKnockbackResistPerLevel() { return knockbackResistPerLevel; }
    public Map<Integer, String> getEquipmentThresholds() { return equipmentThresholds; }
    public double getEnchantMultiplier() { return enchantMultiplier; }
    public double getPotionChancePerLevel() { return potionChancePerLevel; }
    public double getPotionDurationMultiplier() { return potionDurationMultiplier; }
    public int getPotionAmplifierBase() { return potionAmplifierBase; }
    public int getPotionMaxDurationSeconds() { return potionMaxDurationSeconds; }
    public boolean isEliteEnabled() { return eliteEnabled; }
    public double getEliteSpawnChancePercent() { return eliteSpawnChancePercent; }
    public double getEliteHealthMultiplier() { return eliteHealthMultiplier; }
    public double getEliteDamageMultiplier() { return eliteDamageMultiplier; }
    public double getEliteSpeedMultiplier() { return eliteSpeedMultiplier; }
    public double getEliteSize() { return eliteSize; }
    public boolean isEliteAlwaysEquipped() { return eliteAlwaysEquipped; }
    public int getEliteDiamondsMin() { return eliteDiamondsMin; }
    public int getEliteDiamondsMax() { return eliteDiamondsMax; }
    public int getEliteGoldMin() { return eliteGoldMin; }
    public int getEliteGoldMax() { return eliteGoldMax; }
    public List<String> getEliteMusicDiscs() { return eliteMusicDiscs; }
    public int getEliteExpPerLevel() { return eliteExpPerLevel; }
    public boolean isDynamicSizeEnabled() { return dynamicSizeEnabled; }
    public double getDynamicSizeMin() { return dynamicSizeMin; }
    public double getDynamicSizeMax() { return dynamicSizeMax; }
    public boolean isCustomNameEnabled() { return customNameEnabled; }
    public boolean isCustomNameVisible() { return customNameVisible; }
    public String getCustomNameFormat() { return customNameFormat; }
    public String getCustomNameBossFormat() { return customNameBossFormat; }
    public ChatColor getColorLow() { return colorLow; }
    public ChatColor getColorMedium() { return colorMedium; }
    public ChatColor getColorHigh() { return colorHigh; }
    public ChatColor getColorBoss() { return colorBoss; }
    public Set<String> getWorldBlacklist() { return worldBlacklist; }
    public Set<String> getMobWhitelist() { return mobWhitelist; }
    public boolean isDebugMode() { return debugMode; }
}