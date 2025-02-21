package nesoi.network.NClaim.systems.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.ClaimMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static nesoi.network.NClaim.NCoreMain.sendActionBar;

public class ClaimManager implements Listener {

    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    private boolean sendCooldownMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = messageCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastMessageTime >= 15000) {
            player.sendMessage(message);
            messageCooldown.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.equals(toChunk)) return;

        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        String ownerID = cDM.getClaimOwner(toChunk);
        boolean isPvpEnabled = cDM.isClaimSettingEnabled(toChunk, "claim-pvp", false);
        String pvpStatus = isPvpEnabled ? "{GREEN}PvP" : "{RED}PvP";

        if (ownerID != null) {
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(ownerID)).getName();
            sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.claimed-chunk", ownerName, pvpStatus));
        } else {
            sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.unclaimed-chunk"));
        }

    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        Chunk chunk = event.getLocation().getChunk();

        if (event.getEntity() instanceof Creeper) {
            if (!cDM.isClaimSettingEnabled(chunk, "creeper-damage", true)) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof TNTPrimed) {
            if (!cDM.isClaimSettingEnabled(chunk, "tnt-damage", true)) {
                event.setCancelled(true);
            }
            event.blockList().removeIf(block -> {
                Chunk blockChunk = block.getChunk();
                return !cDM.isUnClaimed(blockChunk);
            });

        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        Chunk chunk = event.getLocation().getChunk();
        Entity entity = event.getEntity();

        if (!cDM.isUnClaimed(chunk)) {
            if (entity instanceof Monster) {
                if (!cDM.isClaimSettingEnabled(chunk, "monsters-spawning", true)) {
                    event.setCancelled(true);
                }
            }
            if (entity instanceof Animals) {
                if (!cDM.isClaimSettingEnabled(chunk, "animals-spawning", true)) {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onPlayerInteractWithVillager(PlayerInteractEntityEvent event) {
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Chunk chunk = entity.getLocation().getChunk();

        if (entity instanceof Villager) {
            UUID owner = UUID.fromString(cDM.getClaimOwner(chunk));
            if (owner.equals(player.getUniqueId())) {
                return;
            }

            if (!cDM.isClaimSettingEnabled(chunk, "villager-interacting", false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVillagerInventoryOpen(InventoryOpenEvent event) {
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        HumanEntity player = event.getPlayer();
        Inventory inventory = event.getInventory();

        if (player instanceof Player && inventory.getHolder() instanceof Villager) {
            Chunk chunk = player.getLocation().getChunk();
            UUID owner = UUID.fromString(cDM.getClaimOwner(chunk));

            if (owner.equals(player.getUniqueId())) {
                return;
            }

            if (!cDM.isClaimSettingEnabled(chunk, "villager-interacting", false)) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        final Entity damaged = event.getEntity();
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;

        // if damager and damaged both a player
        if (damager instanceof Player && damaged instanceof Player) {
            Chunk damagerChunk = damager.getLocation().getChunk();
            Chunk damagedChunk = damaged.getLocation().getChunk();

            boolean damagerInClaim = !cDM.isUnClaimed(damagerChunk);
            boolean damagedInClaim = !cDM.isUnClaimed(damagedChunk);

            if (damagedInClaim && !cDM.isClaimSettingEnabled(damagedChunk, "claim-pvp", false)) {
                event.setCancelled(true);
                sendCooldownMessage((Player) damager, NCoreMain.inst().langManager.getMsg("messages.pvp-not-allowed-in-claim"));
            }
            else if (!damagedInClaim && damagerInClaim) {
                event.setCancelled(true);
                sendCooldownMessage((Player) damager, NCoreMain.inst().langManager.getMsg("messages.pvp-not-allowed-in-claim"));
            }
        }

        // if damager is a player and damaged is a mob
        if (damager instanceof Player && !(damaged instanceof Player)) {
            Chunk damagerChunk = damager.getLocation().getChunk();
            Chunk damagedChunk = damaged.getLocation().getChunk();

            boolean damagerInClaim = !cDM.isUnClaimed(damagerChunk);
            boolean damagedInClaim = !cDM.isUnClaimed(damagedChunk);

            if (damagedInClaim && !cDM.isClaimSettingEnabled(damagedChunk, "mob-attacking", false)) {
                event.setCancelled(true);
            }
            else if (!damagedInClaim && damagerInClaim) {
                event.setCancelled(true);
            }
        }

        // if damager is a mob and damaged is a player
        if (!(damager instanceof Player) && damaged instanceof Player) {
            Chunk damagedChunk = damaged.getLocation().getChunk();
            boolean damagedInClaim = !cDM.isUnClaimed(damagedChunk);

            if (damagedInClaim && !cDM.isClaimSettingEnabled(damagedChunk, "mob-attacking", false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;

        if (!cDM.isUnClaimed(chunk)) {
            if (block.getType() == Material.SPAWNER) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-break-spawners")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                }
            } else {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-break-blocks")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                }
            }

            if (block.getType() == Material.BEDROCK) {
                Location blockLocation = block.getLocation();
                if (cDM.isBedrockLocation(blockLocation)) {
                    if (!player.getGameMode().equals(GameMode.CREATIVE) && !player.isSneaking() && !player.isOp()) {
                        event.setCancelled(true);
                    }
                }
            }
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;

        if (!cDM.isUnClaimed(chunk)) {
            if (block.getType() == Material.SPAWNER) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-place-spawners")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                }
            } else {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-place-blocks")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;
        Chunk chunk = block.getChunk();
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;

        if (!cDM.isUnClaimed(chunk)) {

            Material itemInHand = player.getInventory().getItemInMainHand().getType();

            if (itemInHand == Material.WATER_BUCKET ||
                    itemInHand == Material.LAVA_BUCKET ||
                    itemInHand == Material.POWDER_SNOW_BUCKET ||
                    itemInHand == Material.MILK_BUCKET ||
                    itemInHand == Material.AXOLOTL_BUCKET ||
                    itemInHand == Material.COD_BUCKET ||
                    itemInHand == Material.PUFFERFISH_BUCKET ||
                    itemInHand == Material.SALMON_BUCKET ||
                    itemInHand == Material.TROPICAL_FISH_BUCKET ||
                    itemInHand == Material.TADPOLE_BUCKET ||
                    itemInHand == Material.BUCKET) {

                if (cDM.isPlayerUnAllowed(player, chunk, "can-cast-water-or-lava")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                    return;
                }
            }

            Material type = block.getType();

            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-interact-with-chests")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                    return;
                }
            } else if (Tag.BUTTONS.isTagged(type) || Tag.DOORS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) || type == Material.LEVER || Tag.TRAPDOORS.isTagged(type)) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-interact-with-buttons-doors-pressure-plates")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
                    return;
                }
            }

            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BEDROCK) {

                String claimOwner = cDM.getClaimOwner(chunk);
                Block clickedBlock = event.getClickedBlock();
                Location blockLocation = clickedBlock.getLocation();
                Chunk clickedChunk = clickedBlock.getChunk();

                if (claimOwner != null && player.getUniqueId().toString().equals(claimOwner)) {
                    if (cDM.isBedrockLocation(blockLocation)) {
                        new ClaimMenu(player, clickedChunk);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlockClicked().getChunk();
        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;

        if (!cDM.isUnClaimed(chunk)) {
            if (cDM.isPlayerUnAllowed(player, chunk, "can-cast-water-or-lava")) {
                event.setCancelled(true);
                sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
            }
        }


    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();

        if (fromBlock.getType() != Material.WATER && fromBlock.getType() != Material.LAVA) {
            return;
        }

        Chunk fromChunk = fromBlock.getChunk();
        Chunk toChunk = toBlock.getChunk();

        ClaimDataManager cDM = NCoreMain.inst().claimDataManager;
        String fromOwner = cDM.getClaimOwner(fromChunk);
        String toOwner = cDM.getClaimOwner(toChunk);

        if (fromOwner == null && toOwner != null) {
            event.setCancelled(true);
        }
    }
}
