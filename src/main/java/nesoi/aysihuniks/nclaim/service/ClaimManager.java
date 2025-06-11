package nesoi.network.NClaim.systems.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.inside.ClaimMenu;
import nesoi.network.NClaim.menus.claim.admin.inside.ManageClaimMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.ClaimSetting;
import nesoi.network.NClaim.model.CoopPermission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.nandayo.DAPI.object.DMaterial;

import java.util.*;

import static nesoi.network.NClaim.NCoreMain.sendActionBar;

public class ClaimManager implements Listener {

    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    private void sendCooldownMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = messageCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastMessageTime >= 15000) {
            player.sendMessage(message);
            messageCooldown.put(playerUUID, currentTime);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.equals(toChunk)) return;
        Claim claim = Claim.getClaim(toChunk);

        if (claim == null) {
            sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.unclaimed-chunk"));
            return;
        }

        boolean isPvpEnabled = claim.getSettingState(ClaimSetting.Setting.CLAIM_PVP);
        String pvpStatus = isPvpEnabled ? "{GREEN}PvP" : "{RED}PvP";

        UUID ownerUUID = claim.getOwner();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

        sendActionBar(player, NCoreMain.inst().langManager.getMsg("action-bar.claimed-chunk", owner.getName(), pvpStatus));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> affectedBlocks = event.blockList();
        Iterator<Block> iterator = affectedBlocks.iterator();

        boolean isCreeper = event.getEntity() instanceof Creeper;
        boolean isTNT = event.getEntity() instanceof TNTPrimed;

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Claim blockClaim = Claim.getClaim(block.getChunk());

            if (blockClaim != null) {
                if ((isCreeper && !blockClaim.getSettingState(ClaimSetting.Setting.CREEPER_DAMAGE)) ||
                        (isTNT && !blockClaim.getSettingState(ClaimSetting.Setting.TNT_DAMAGE))) {
                    iterator.remove();
                }
            }
        }
    }


    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        Claim claim = Claim.getClaim(chunk);
        Entity entity = event.getEntity();

        if (claim != null) {
            if (entity instanceof Monster) {
                if (!claim.getSettingState(ClaimSetting.Setting.MONSTER_SPAWNING)) {
                    event.setCancelled(true);
                }
            }
            if (entity instanceof Animals) {
                if (!claim.getSettingState(ClaimSetting.Setting.ANIMAL_SPAWNING)) {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onPlayerInteractWithVillager(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Chunk chunk = entity.getLocation().getChunk();
        Claim claim = Claim.getClaim(chunk);

        if (entity instanceof Villager) {
            UUID owner = claim.getOwner();
            if (owner.equals(player.getUniqueId())) return;

            if (player.hasPermission("nclaim.bypass")) return;

            if (!claim.getSettingState(ClaimSetting.Setting.VILLAGER_INTERACTION)) {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onVillagerInventoryOpen(InventoryOpenEvent event) {
        HumanEntity player = event.getPlayer();
        Inventory inventory = event.getInventory();

        if (player instanceof Player && inventory.getHolder() instanceof Villager) {
            Chunk chunk = player.getLocation().getChunk();
            UUID owner = Claim.getClaim(chunk).getOwner();

            if (owner.equals(player.getUniqueId())) return;

            if (player.hasPermission("nclaim.bypass")) return;

            if (!Claim.getClaim(chunk).getSettingState(ClaimSetting.Setting.VILLAGER_INTERACTION)) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();
        Chunk damagedChunk = damaged.getLocation().getChunk();
        Claim damagedClaim = Claim.getClaim(damagedChunk);

        if (damager instanceof Player) {
            Player attacker = (Player) damager;
            boolean hasBypass = attacker.hasPermission("nclaim.bypass");
            Chunk damagerChunk = damager.getLocation().getChunk();
            Claim damagerClaim = Claim.getClaim(damagerChunk);

            if (damaged instanceof Player) {
                if ((damagedClaim != null && !damagedClaim.getSettingState(ClaimSetting.Setting.CLAIM_PVP)) ||
                        (damagerClaim != null && !damagerClaim.getSettingState(ClaimSetting.Setting.CLAIM_PVP))) {
                    event.setCancelled(true);
                    sendCooldownMessage(attacker, NCoreMain.inst().langManager.getMsg("messages.error.pvp-not-allowed"));
                }
            } else if (damagedClaim != null) {
                if (!hasBypass && !damagedClaim.getOwner().equals(attacker.getUniqueId()) &&
                        !damagedClaim.getSettingState(ClaimSetting.Setting.MOB_ATTACKING)) {
                    event.setCancelled(true);
                }
            }
        } else if (damaged instanceof Player && damagedClaim != null) {
            Player victim = (Player) damaged;
            if (!damagedClaim.getOwner().equals(victim.getUniqueId()) &&
                    !damagedClaim.getSettingState(ClaimSetting.Setting.MOB_ATTACKING)) {
                event.setCancelled(true);
            }
        }

        if (damager instanceof Creeper && damagedClaim != null &&
                !damagedClaim.getSettingState(ClaimSetting.Setting.CREEPER_DAMAGE)) {
            event.setCancelled(true);
        }

        if (damager instanceof TNTPrimed && damagedClaim != null &&
                !damagedClaim.getSettingState(ClaimSetting.Setting.TNT_DAMAGE)) {
            event.setCancelled(true);
        }
    }





    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        Claim claim = Claim.getClaim(chunk);

        if (claim != null) {
            if (claim.getOwner().equals(player.getUniqueId())) return;

            if (player.hasPermission("nclaim.bypass")) return;

            if (claim.getCoopPlayers().contains(player.getUniqueId())) {
                if (block.getType() == Material.SPAWNER) {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_BREAK_SPAWNER)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                } else {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_BREAK_BLOCK)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                }
            } else {
                event.setCancelled(true);
                sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            }

            if (block.getType() == Material.BEDROCK) {
                Location blockLocation = block.getLocation();
                if (claim.getBedrockLocation().equals(blockLocation)) {
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
        Claim claim = Claim.getClaim(chunk);

        if (claim != null) {
            if (claim.getOwner().equals(player.getUniqueId())) return;

            if (player.hasPermission("nclaim.bypass")) return;

            if (claim.getCoopPlayers().contains(player.getUniqueId())) {
                if (block.getType() == Material.SPAWNER) {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_PLACE_SPAWNER)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                } else {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_PLACE_BLOCK)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                }
            } else {
                event.setCancelled(true);
                sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;
        Chunk chunk = block.getChunk();
        Claim claim = Claim.getClaim(chunk);

        if (claim != null) {

            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BEDROCK) {
                UUID claimOwner = claim.getOwner();
                Location clickedBlock = event.getClickedBlock().getLocation();
                if (claimOwner.equals(player.getUniqueId())) {
                    if (player.isSneaking() && claim.getBedrockLocation().equals(clickedBlock) && player.hasPermission("nclaim.admin")) {
                        new ManageClaimMenu(player, claim);
                    }
                    else if (claim.getBedrockLocation().equals(clickedBlock)) {
                        new ClaimMenu(player, claim);
                    }
                } else if (player.hasPermission("nclaim.admin")) {
                    if (claim.getBedrockLocation().equals(clickedBlock)) {
                        new ManageClaimMenu(player, claim);
                    }
                }
            }

            if (claim.getOwner().equals(player.getUniqueId())) return;
            if (player.hasPermission("nclaim.bypass")) return;

            Material itemInHand = player.getInventory().getItemInMainHand().getType();

            if (itemInHand == Material.WATER_BUCKET ||
                    itemInHand == Material.LAVA_BUCKET ||
                    itemInHand == DMaterial.POWDER_SNOW_BUCKET.get() ||
                    itemInHand == Material.MILK_BUCKET ||
                    itemInHand == DMaterial.AXOLOTL_BUCKET.get() ||
                    itemInHand == Material.COD_BUCKET ||
                    itemInHand == Material.PUFFERFISH_BUCKET ||
                    itemInHand == Material.SALMON_BUCKET ||
                    itemInHand == Material.TROPICAL_FISH_BUCKET ||
                    itemInHand == DMaterial.TADPOLE_BUCKET.get() ||
                    itemInHand == Material.BUCKET) {

                if (claim.getCoopPlayers().contains(player.getUniqueId())) {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_CAST_WATER_AND_LAVA)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                }
            }

            Material type = block.getType();

            if (claim.getCoopPlayers().contains(player.getUniqueId())) {
                if (type == Material.CHEST || type == Material.TRAPPED_CHEST ||type == Material.ENDER_CHEST) {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_INTERACT_WITH_CHEST)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                } else if (Tag.BUTTONS.isTagged(type) || Tag.DOORS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) || type == Material.LEVER || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
                    if (!claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_INTERACT_WITH_BUTTON_DOOR_PRESSURE_PLATE)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                    }
                }
            } else {
                event.setCancelled(true);
                sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlockClicked().getChunk();
        Claim claim = Claim.getClaim(chunk);


        if (claim != null) {
            if (claim.getOwner().equals(player.getUniqueId())) return;

            if (player.hasPermission("nclaim.bypass")) return;

            if (claim.getCoopPlayers().contains(player.getUniqueId())) {
                if (claim.getCoopPermissionState(player.getUniqueId(), CoopPermission.Permission.CAN_CAST_WATER_AND_LAVA)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
                }
            } else {
                event.setCancelled(true);
                sendCooldownMessage(player, NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
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

        if (Claim.getClaim(fromChunk) == null && Claim.getClaim(toChunk) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedstoneActivate(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Chunk blockChunk = block.getChunk();
        Claim sourceClaim = Claim.getClaim(blockChunk);

        for (BlockFace face : BlockFace.values()) {
            Block relative = block.getRelative(face);
            Claim targetClaim = Claim.getClaim(relative.getChunk());

            if (targetClaim == null) continue;

            if (sourceClaim == null || !sourceClaim.getOwner().equals(targetClaim.getOwner())) {
                if (!targetClaim.getCoopPlayers().contains(sourceClaim != null ? sourceClaim.getOwner() : null)) {
                    event.setNewCurrent(event.getOldCurrent());
                    return;
                }
            }
        }
    }


}
