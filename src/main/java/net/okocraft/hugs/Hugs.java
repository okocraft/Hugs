package net.okocraft.hugs;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@NullMarked
public class Hugs extends JavaPlugin implements Listener {

    private static final Sound HUG_SOUND =
            Sound.sound(SoundEventKeys.ENTITY_CAT_PURR, Sound.Source.MASTER, 0.96f, 1.0f);

    private final Map<UUID, Long> lastHugTime = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        var pluginManager = getServer().getPluginManager();

        try {
            Messages.register(this);
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "An error occurred while loading messages", e);
            pluginManager.disablePlugin(this);
            return;
        }

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> HugCommand.register(event.registrar(), this::isInCoolDown, this::hug));
        pluginManager.registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        lastHugTime.clear();
        HandlerList.unregisterAll((Listener) this);
        Messages.unregister();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        var player = e.getPlayer();
        var entity = e.getRightClicked();

        if (
                e.getHand() != EquipmentSlot.HAND ||
                !player.isSneaking() ||
                !(entity instanceof LivingEntity) ||
                !player.hasPermission("hugs.hug") ||
                this.isInCoolDown(player)
        ) {
            return;
        }

        this.hug(player, List.of(entity));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        lastHugTime.remove(e.getPlayer().getUniqueId());
    }

    private boolean isInCoolDown(Player player) {
        return System.currentTimeMillis() - lastHugTime.getOrDefault(player.getUniqueId(), 0L) < 1000;
    }

    private void hug(CommandSender source, List<? extends Entity> targets) {
        Player sourcePlayer = source instanceof Player p ? p : null;
        if (sourcePlayer != null) {
            this.lastHugTime.put(sourcePlayer.getUniqueId(), System.currentTimeMillis());
        }

        notifySource(source, targets);

        targets.forEach(target -> {
            if (target instanceof Player targetPlayer) {
                notifyTargetPlayer(source, targetPlayer);
            }
        });
    }

    private static void notifySource(CommandSender source, List<? extends Entity> targets) {
        Entity singleTarget = targets.size() == 1 ? targets.getFirst() : null;

        if (source instanceof Player player) {
            Location targetLocation = singleTarget != null ? singleTarget.getLocation() : null;
            spawnHugEffect(player, targetLocation);
        }

        if (singleTarget == null) {
            source.sendMessage(Messages.HUG_MULTIPLE.apply(targets.size()));
        } else if (!source.equals(singleTarget)) {
            source.sendMessage(
                    singleTarget instanceof Player ?
                            Messages.HUG_PLAYER.apply(singleTarget.getName()) :
                            Messages.HUG_ENTITY.apply(singleTarget.getName())
            );
        }
    }

    private static void notifyTargetPlayer(CommandSender source, Player target) {
        if (source.equals(target)) {
            target.sendMessage(Messages.HUG_SELF);
            return;
        }

        Location sourceLocation = switch (source) {
            case Entity entity -> entity.getLocation();
            case BlockCommandSender block -> block.getBlock().getLocation();
            default -> null;
        };

        spawnHugEffect(target, sourceLocation);
        target.sendMessage(Messages.HUG_HUGGED.apply(source.getName()));
    }

    private static void spawnHugEffect(Player target, @Nullable Location location) {
        if (location != null && isInViewDistance(target, location)) {
            target.spawnParticle(Particle.HEART, location, 13, 0.5, 0.5, 0.5);
        }
        target.playSound(HUG_SOUND);
    }

    private static boolean isInViewDistance(Player player, Location location) {
        if (!player.getWorld().equals(location.getWorld())) {
            return false;
        }

        Location playerLocation = player.getLocation();
        int chunkDistance = Math.max(
                Math.abs((playerLocation.getBlockX() >> 4) - (location.getBlockX() >> 4)),
                Math.abs((playerLocation.getBlockZ() >> 4) - (location.getBlockZ() >> 4))
        );

        return chunkDistance <= Math.min(player.getSendViewDistance(), player.getClientViewDistance());
    }
}
