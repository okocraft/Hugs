package net.okocraft.hugs;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

import java.util.HashMap;
import java.util.Map;

public class Hugs extends JavaPlugin implements Listener {

    private final Map<Player, Long> lastHugTime = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        lastHugTime.clear();
        HandlerList.unregisterAll((Listener) this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();

        if (!e.getHand().equals(EquipmentSlot.HAND) || !player.isSneaking() || !(entity instanceof LivingEntity)) {
            return;
        }

        if (!player.hasPermission("hugs.hug")) {
            sendMessage(player, "&c権限がありません。");
            return;
        }

        hug(player, entity);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        lastHugTime.remove(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        if (!sender.hasPermission("hugs.command")) {
            sendMessage(sender, "&c権限がありません。");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender, "&b/hug <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);

        if (target == null) {
            sendMessage(sender, "&7あなたが抱擁したかった人はここにはいない...");
            return true;
        }

        Player player = (Player) sender;

        hug(player, target);
        return true;
    }

    private void hug(Player player, Entity entity) {
        long last = lastHugTime.getOrDefault(player, 0L);

        if (System.currentTimeMillis() - last < 1000) {
            return;
        } else {
            lastHugTime.put(player, System.currentTimeMillis());
        }

        spawnEffects(player, entity.getLocation());

        if (player.getName().equals(entity.getName())) {
            sendMessage(player, "&7癒えぬ痛みに、あなたは心を慰めようとした。無常を味わった。");
            return;
        }

        if (entity instanceof Player) {
            spawnEffects((Player) entity, player.getLocation());

            sendMessage(player, "&b" + entity.getName() + "&7 を抱きしめちゃった!");
            sendMessage(entity, "&b" + player.getName() + "&7 に抱きしめられちゃった!");
        } else {
            sendMessage(player, "&e" + entity.getName() + "&7 を抱きしめちゃった!");
        }
    }

    private void sendMessage(CommandSender receiver, String msg) {
        receiver.sendMessage(("&8[&dハグ&8]&r " + msg).replace("&", "§"));
    }

    private void spawnEffects(Player player, Location loc) {
        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, SoundCategory.MASTER, 0.96f, 1.0f);
        player.spawnParticle(Particle.HEART, loc.getX(), loc.getY() + 0.5, loc.getZ(), 13, 0.5, 0.5, 0.5);
    }
}
