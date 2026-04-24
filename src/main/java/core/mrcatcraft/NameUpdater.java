package core.mrcatcraft;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utility class responsible for setting the custom display name of HardMob entities.
 * <p>
 * The name can show current health, max health, the mob's name, and its level (or "BOSS" for elites).
 * A simple health cache prevents redundant updates when the health hasn't changed significantly.
 * </p>
 */
public class NameUpdater {

    // WeakHashMap ensures entries are garbage collected when the entity no longer exists
    private static final Map<LivingEntity, Double> lastHealthCache = new WeakHashMap<>();

    /**
     * Updates the custom name if health has changed (or if force == true).
     *
     * @param entity the entity to rename
     * @param force  if true, update even if health hasn't changed
     */
    public static void updateName(LivingEntity entity, boolean force) {
        HardMob plugin = HardMob.getInstance();
        ConfigManager cfg = plugin.getConfigManager();

        if (!cfg.isCustomNameEnabled()) return;
        if (!(entity instanceof Mob mob)) return;

        int level = plugin.getMobLevelManager().getLevelFromMob(mob);
        boolean elite = plugin.getMobLevelManager().isElite(mob);
        if (level == 0 && !elite) return; // Not a HardMob entity

        double health = mob.getHealth();
        Double lastHealth = lastHealthCache.get(mob);
        if (!force && lastHealth != null && Math.abs(lastHealth - health) < 0.01) {
            return; // insignificant change
        }
        lastHealthCache.put(mob, health);

        double maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        String healthStr = formatNumber(health);
        String maxHealthStr = formatNumber(maxHealth);
        String mobName = formatMobName(mob.getType().name());

        String format;
        if (elite) {
            format = cfg.getCustomNameBossFormat();
            format = format.replace("%boss%", "BOSS");
        } else {
            format = cfg.getCustomNameFormat();
            ChatColor levelColor = getLevelColor(level);
            format = format.replace("%level%", levelColor + Integer.toString(level));
        }
        format = format.replace("%health%", healthStr)
                .replace("%max_health%", maxHealthStr)
                .replace("%name%", mobName);

        String coloredName = ChatColor.translateAlternateColorCodes('&', format);
        mob.setCustomName(coloredName);
        mob.setCustomNameVisible(cfg.isCustomNameVisible());
    }

    /**
     * Updates the name without forcing (respects health cache).
     *
     * @param entity the entity
     */
    public static void updateName(LivingEntity entity) {
        updateName(entity, false);
    }

    /**
     * Forces an immediate name update, ignoring the health cache.
     *
     * @param entity the entity
     */
    public static void forceUpdateName(LivingEntity entity) {
        updateName(entity, true);
    }

    /**
     * Nicely formats a number: shows an integer if it is a whole number, otherwise one decimal.
     */
    private static String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return Integer.toString((int) Math.round(value));
        }
        return String.format("%.1f", value);
    }

    /**
     * Converts an entity type name (e.g. "ZOMBIFIED_PIGLIN") to a readable form ("Zombified Piglin").
     */
    private static String formatMobName(String typeName) {
        String name = typeName.toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /**
     * Returns a {@link ChatColor} for a given level based on the config thresholds.
     */
    private static ChatColor getLevelColor(int level) {
        HardMob plugin = HardMob.getInstance();
        ConfigManager cfg = plugin.getConfigManager();
        if (level <= 20) return cfg.getColorLow();
        if (level <= 50) return cfg.getColorMedium();
        if (level <= 80) return cfg.getColorHigh();
        return cfg.getColorBoss();
    }
}