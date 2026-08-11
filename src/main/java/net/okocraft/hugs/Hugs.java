package net.okocraft.hugs;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Hugs extends JavaPlugin implements Listener {

    private static final Sound HUG_SOUND =
            Sound.sound(org.bukkit.Sound.ENTITY_CAT_PURR, Sound.Source.MASTER, 0.96f, 1.0f);

    private static final ParticleBuilder HUG_PARTICLE =
            new ParticleBuilder(Particle.HEART).offset(0.5, 0.5, 0.5).count(13);

    private final Map<Player, Long> lastHugTime = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        var pluginManager = getServer().getPluginManager();

        try {
            Messages.register(this);
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "An error occurred while loading messages", e);
            pluginManager.disablePlugin(this);
        }

        registerCommands();
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
        var hand = e.getHand();

        if (hand == EquipmentSlot.HAND && player.isSneaking() && entity instanceof LivingEntity) {
            processRightClick(player, entity);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        lastHugTime.remove(e.getPlayer());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("hug")
                                .executes(context -> {
                                    var sender = context.getSource().getSender();

                                    if (!sender.hasPermission("hugs.command")) {
                                        sender.sendMessage(Messages.NO_PERMISSION);
                                    } else if (sender instanceof Player) {
                                        sender.sendMessage(Messages.COMMAND_USAGE);
                                    } else {
                                        sender.sendMessage(Messages.ONLY_PLAYER);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(context -> {
                                            var sender = context.getSource().getSender();

                                            if (!sender.hasPermission("hugs.command")) {
                                                sender.sendMessage(Messages.NO_PERMISSION);
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            if (!(sender instanceof Player player)) {
                                                sender.sendMessage(Messages.ONLY_PLAYER);
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            var target = context
                                                    .getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(context.getSource())
                                                    .getFirst();

                                            hug(player, target);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .build(),
                        "Hug other players by command"
                )
        );
    }

    private void processRightClick(@NotNull Player player, @NotNull Entity target) {
        if (player.hasPermission("hugs.hug")) {
            hug(player, target);
        }
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

        if (entity instanceof Player target) {
            HUG_PARTICLE.location(player.getLocation()).receivers(target).spawn();
            target.playSound(HUG_SOUND);

            player.sendMessage(Messages.HUG_PLAYER.apply(target.getName()));
            target.sendMessage(Messages.HUG_HUGGED.apply(player.getName()));
        } else {
            player.sendMessage(Messages.HUG_ENTITY.apply(entity.getName()));
        }
    }
}
