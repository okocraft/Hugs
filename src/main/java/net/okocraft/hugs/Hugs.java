package net.okocraft.hugs;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
import org.bukkit.command.Command;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Hugs extends JavaPlugin implements Listener {

    private static final Sound HUG_SOUND =
            Sound.sound(org.bukkit.Sound.ENTITY_CAT_PURR, Sound.Source.MASTER, 0.96f, 1.0f);

    private static final ParticleBuilder HUG_PARTICLE =
            new ParticleBuilder(Particle.HEART).offset(0.5, 0.5, 0.5).count(13);

    private final Map<Player, Long> lastHugTime = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onEnable() {
        var pluginManager = getServer().getPluginManager();

        try {
            Messages.register(this);
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "An error occurred while loading messages", e);
            pluginManager.disablePlugin(this);
        }

        pluginManager.registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        lastHugTime.clear();
        HandlerList.unregisterAll((Listener) this);
        Messages.unregister();
        executor.shutdownNow();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        var player = e.getPlayer();
        var entity = e.getRightClicked();
        var hand = e.getHand();

        if (hand == EquipmentSlot.HAND && player.isSneaking() && entity instanceof LivingEntity) {
            executor.submit(() -> processRightClick(player, entity));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        lastHugTime.remove(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.ONLY_PLAYER);
            return true;
        }

        executor.submit(() -> processCommand((Player) sender, args));
        return true;
    }

    private void processRightClick(@NotNull Player player, @NotNull Entity target) {
        if (player.hasPermission("hugs.hug")) {
            hug(player, target);
        }
    }

    private void processCommand(@NotNull Player player, @NotNull String[] args) {
        if (!player.hasPermission("hugs.command")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return;
        }

        if (args.length == 0) {
            player.sendMessage(Messages.COMMAND_USAGE);
            return;
        }

        var target = getServer().getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return;
        }

        hug(player, target);
    }

    private void hug(Player player, Entity entity) {
        long last = lastHugTime.getOrDefault(player, 0L);

        if (System.currentTimeMillis() - last < 1000) {
            return;
        } else {
            lastHugTime.put(player, System.currentTimeMillis());
        }

        HUG_PARTICLE.location(entity.getLocation()).receivers(player).spawn();
        player.playSound(HUG_SOUND);

        if (player.getName().equals(entity.getName())) {
            player.sendMessage(Messages.HUG_SELF);
            return;
        }

        if (entity instanceof Player) {
            var target = (Player) entity;
            HUG_PARTICLE.location(player.getLocation()).receivers(target).spawn();
            target.playSound(HUG_SOUND);

            player.sendMessage(Messages.HUG_PLAYER.apply(target.getName()));
            target.sendMessage(Messages.HUG_HUGGED.apply(player.getName()));
        } else {
            player.sendMessage(Messages.HUG_ENTITY.apply(entity.getName()));
        }
    }
}
