package nesoi.network.NClaim.systems.claim;

import nesoi.network.NClaim.Config;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.ClaimMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nesoi.network.NClaim.NCoreMain.sendActionBar;

public class ClaimManager implements Listener {

    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    private boolean sendCooldownMessage(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = messageCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastMessageTime >= 15000) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.dont-have-permission"));
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

        Config config = NCoreMain.inst().config;
        if (ownerID != null) {

            if (ownerID.equals(player.getUniqueId().toString()) || cDM.isCoopMember(toChunk, player.getUniqueId())) {
                sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.our-chunk"));
            } else {
                sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.claimed-chunk"));
            }
        } else {
            sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.unclaimed-chunk"));
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
                    sendCooldownMessage(player);
                }
            } else {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-break-blocks")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player);
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
                    sendCooldownMessage(player);
                }
            } else {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-place-blocks")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player);
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
                    sendCooldownMessage(player);
                    return;
                }
            }

            Material type = block.getType();

            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-interact-with-chests")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player);
                    return;
                }
            } else if (Tag.BUTTONS.isTagged(type) || Tag.DOORS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) || type == Material.LEVER || Tag.TRAPDOORS.isTagged(type)) {
                if (cDM.isPlayerUnAllowed(player, chunk, "can-interact-with-buttons-doors-pressure-plates")) {
                    event.setCancelled(true);
                    sendCooldownMessage(player);
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
                sendCooldownMessage(player);
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
