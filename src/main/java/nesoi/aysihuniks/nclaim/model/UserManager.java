package nesoi.aysihuniks.nclaim.model;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.utils.HeadManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.nandayo.dapi.Util;

import java.util.UUID;

public class UserManager implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        User.loadUser(playerUUID);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String texture = NClaim.inst().getHeadManager().getSkinTextureValue(playerUUID, true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (texture != null) {
                                User user = User.getUser(playerUUID);
                                if (user != null && !texture.equals(user.getSkinTexture())) {
                                    user.setSkinTexture(texture);
                                    User.saveUser(playerUUID);
                                }
                                NClaim.inst().getHeadManager().getSkinTextureCache().put(playerUUID, texture);
                                ItemStack head = NClaim.inst().getHeadManager().createHeadWithTexture(texture);
                                NClaim.inst().getHeadManager().getHeadCache().put(playerUUID, head);
                            } else {
                                NClaim.inst().getHeadManager().getSkinTextureCache().put(playerUUID, null);
                                ItemStack defaultHead = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
                                NClaim.inst().getHeadManager().getHeadCache().put(playerUUID, defaultHead);
                            }
                        }
                    }.runTask(NClaim.inst());
                } catch (Exception e) {
                    Util.log("UserManager failed to fetch skin texture for " + event.getPlayer().getName() + ": " + e.getMessage());
                }
            }
        }.runTaskLaterAsynchronously(NClaim.inst(), 200L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        NClaim.inst().getHeadManager().getSkinTextureCache().remove(playerUUID);
        NClaim.inst().getHeadManager().getHeadCache().remove(playerUUID);

        User.saveUser(playerUUID);
    }
}
