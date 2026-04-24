package core.mrcatcraft;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages mob levels: calculates the maximum possible level per world,
 * rolls a random level according to the configured distribution,
 * and applies the corresponding attribute modifiers and potion effects.
 */
public class MobLevelManager {

    private final HardMob plugin;
    private final NamespacedKey levelKey;
    private final NamespacedKey eliteKey;

    /**
     * Creates a new level manager and initialises the persistent data keys.
     *
     * @param plugin the main plugin instance
     */
    public MobLevelManager(HardMob plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "level");
        this.eliteKey = new NamespacedKey(plugin, "elite");
    }

    /** Called on /hardmob reload; currently nothing needs to be reset. */
    public void reload() {}

    /**
     * Computes the maximum possible level in the given world based on the world's age.
     * <p>
     * Formula: {@code maxLevel = A - B * exp(-days / C)}<br>
     * where {@code days = world.getFullTime() / 24000.0}.
     * The result is clamped between 1 and 99.
     *
     * @param world the world to check
     * @return the maximum level (1–99)
     */
    public int calculateMaxLevel(World world) {
        long fullTime = world.getFullTime();
        double days = fullTime / 24000.0;
        ConfigManager cfg = plugin.getConfigManager();
        double exponent = Math.exp(-days / cfg.getFormulaC());
        double level = cfg.getFormulaA() - cfg.getFormulaB() * exponent;
        int maxLevel = (int) Math.round(level);
        return Math.min(99, Math.max(1, maxLevel));
    }

    /**
     * Rolls a random level for a newly spawned mob.
     * The distribution can be uniform, weighted towards low levels, or weighted towards high levels.
     *
     * @param world the world where the mob spawns (used to determine max level)
     * @return a level between 1 and the current max level (inclusive)
     */
    public int rollLevel(World world) {
        int maxLvl = calculateMaxLevel(world);
        String dist = plugin.getConfigManager().getDistribution();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        switch (dist) {
            case "uniform":
                return random.nextInt(1, maxLvl + 1);
            case "weighted_low":
                // Squaring a uniform random value makes lower levels more likely
                double rLow = random.nextDouble();
                return 1 + (int) (rLow * rLow * (maxLvl - 1));
            case "weighted_high":
                double rHigh = random.nextDouble();
                return maxLvl - (int) (rHigh * rHigh * (maxLvl - 1));
            default:
                return 1;
        }
    }

    /**
     * Applies level‑ and elite‑based attribute modifiers to a mob.
     * <p>
     * If the mob has a positive level and the level system is enabled, the multipliers
     * are calculated as:<br>
     * {@code multiplier = 1 + (level - 1) * percent_per_level / 100}.
     * If the mob is elite and elite settings are enabled, further multipliers are applied.
     *
     * @param mob     the target mob
     * @param level   the mob's level (0 means no level modifiers)
     * @param isElite whether the mob is elite
     */
    public void applyLevelModifiers(Mob mob, int level, boolean isElite) {
        ConfigManager cfg = plugin.getConfigManager();
        double healthMult = 1.0;
        double damageMult = 1.0;
        double speedMult = 1.0;
        double knockbackResist = 0.0;

        if (level > 0 && cfg.isLevelEnabled()) {
            healthMult = 1.0 + (level - 1) * cfg.getHealthPercentPerLevel() / 100.0;
            damageMult = 1.0 + (level - 1) * cfg.getDamagePercentPerLevel() / 100.0;
            speedMult = 1.0 + (level - 1) * cfg.getSpeedPercentPerLevel() / 100.0;
            knockbackResist = (level - 1) * cfg.getKnockbackResistPerLevel();
        }

        if (isElite && cfg.isEliteEnabled()) {
            healthMult *= cfg.getEliteHealthMultiplier();
            damageMult *= cfg.getEliteDamageMultiplier();
            speedMult *= cfg.getEliteSpeedMultiplier();
        }

        // Apply health while preserving existing health percentage
        AttributeInstance healthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double newMax = healthAttr.getBaseValue() * healthMult;
            double oldHealth = mob.getHealth();
            if (oldHealth > 0) {
                double oldMax = healthAttr.getBaseValue() / healthMult; // original base
                double percent = oldHealth / oldMax;
                healthAttr.setBaseValue(newMax);
                mob.setHealth(newMax * percent);
            } else {
                healthAttr.setBaseValue(newMax);
                mob.setHealth(newMax);
            }
        }

        AttributeInstance damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * damageMult);
        }

        AttributeInstance speedAttr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * speedMult);
        }

        AttributeInstance knockbackAttr = mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(Math.min(1.0, knockbackResist));
        }

        // Potion effects are only applied if the level system is active
        if (level > 0 && cfg.isLevelEnabled()) {
            applyPotionEffects(mob, level);
        }
    }

    /**
     * Gives the mob a random beneficial potion effect if the level‑based chance succeeds.
     * The effect is skipped if a stronger or longer‑lasting effect of the same type is already active.
     *
     * @param mob   the target mob
     * @param level the mob's level
     */
    private void applyPotionEffects(Mob mob, int level) {
        ConfigManager cfg = plugin.getConfigManager();
        double chance = level * cfg.getPotionChancePerLevel() / 100.0;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        PotionEffectType[] possibleEffects = {
                PotionEffectType.SPEED,
                PotionEffectType.STRENGTH,
                PotionEffectType.RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.REGENERATION
        };
        PotionEffectType type = possibleEffects[ThreadLocalRandom.current().nextInt(possibleEffects.length)];
        int amplifier = cfg.getPotionAmplifierBase() + level / 20;
        int durationTicks = 20 * 60 * (1 + (int)(level * cfg.getPotionDurationMultiplier() / 10));
        int maxTicks = cfg.getPotionMaxDurationSeconds() * 20;
        if (durationTicks > maxTicks) durationTicks = maxTicks;

        // Avoid overwriting a better existing effect
        if (mob.hasPotionEffect(type)) {
            PotionEffect existing = mob.getPotionEffect(type);
            if (existing.getAmplifier() >= amplifier && existing.getDuration() >= durationTicks) {
                return;
            }
        }
        mob.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
    }

    /**
     * Reads the stored level from an entity's persistent data container.
     *
     * @param e the entity
     * @return the level, or 0 if absent
     */
    public int getLevelFromMob(Entity e) {
        Integer value = e.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    /**
     * Checks whether an entity is marked as elite.
     *
     * @param e the entity
     * @return true if elite, false otherwise
     */
    public boolean isElite(Entity e) {
        Boolean value = e.getPersistentDataContainer().get(eliteKey, PersistentDataType.BOOLEAN);
        return value != null && value;
    }

    public NamespacedKey getLevelKey() { return levelKey; }
    public NamespacedKey getEliteKey() { return eliteKey; }
}