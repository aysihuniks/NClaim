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

import java.util.UUID;

public class UserManager implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        User.loadUser(playerUUID);

        Bukkit.getScheduler().runTaskLaterAsynchronously(NClaim.inst(), () -> {
            try {
                User user = User.getUser(playerUUID);
                if (user != null) {
                    String texture = NClaim.inst().getHeadManager().getSkinTextureValue(playerUUID);
                    if (texture != null && !texture.equals(user.getSkinTexture())) {
                        Bukkit.getScheduler().runTask(NClaim.inst(), () -> {
                            user.setSkinTexture(texture);
                            User.saveUser(playerUUID);
                            NClaim.inst().getHeadManager().getSkinTextureCache().put(playerUUID, texture);
                            ItemStack head = NClaim.inst().getHeadManager().createHeadWithTexture(texture);
                            NClaim.inst().getHeadManager().getHeadCache().put(playerUUID, head);
                        });
                    } else if (texture != null) {
                        Bukkit.getScheduler().runTask(NClaim.inst(), () -> {
                            NClaim.inst().getHeadManager().getSkinTextureCache().put(playerUUID, texture);
                            ItemStack head = NClaim.inst().getHeadManager().createHeadWithTexture(texture);
                            NClaim.inst().getHeadManager().getHeadCache().put(playerUUID, head);
                        });
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[UserManager] Failed to fetch skin texture for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        }, 200L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        NClaim.inst().getHeadManager().getSkinTextureCache().remove(playerUUID);
        NClaim.inst().getHeadManager().getHeadCache().remove(playerUUID);

        User.saveUser(playerUUID);
    }
}
