package nesoi.aysihuniks.nclaim.service;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimEnterEvent;
import nesoi.aysihuniks.nclaim.api.events.ClaimLeaveEvent;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.ui.claim.management.ClaimManagementMenu;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminClaimManagementMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.Tag;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;

@RequiredArgsConstructor
public class ClaimManager implements Listener {
    private final NClaim plugin;
    private final ClaimCoopManager coopManager;
    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    private void sendCooldownMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = messageCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastMessageTime >= 15000) {
            ChannelType.CHAT.send(player, message);
            messageCooldown.put(playerUUID, currentTime);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.equals(toChunk)) return;

        if (NClaim.inst().getNconfig().getBlacklistedWorlds().contains(player.getWorld().getName())) return;

        if (NClaim.inst().isWorldGuardEnabled()) {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            World world = event.getTo().getWorld();
            if (world != null) {
                RegionManager regions = container.get(BukkitAdapter.adapt(world));

                if (regions != null) {
                    ApplicableRegionSet regionSet = regions.getApplicableRegions(BukkitAdapter.asBlockVector(event.getTo()));
                    for (ProtectedRegion region : regionSet) {
                        if (NClaim.inst().getNconfig().getBlacklistedRegions().contains(region.getId())) {
                            return;
                        }
                    }
                }
            }
        }

        Claim fromClaim = Claim.getClaim(fromChunk);
        Claim toClaim = Claim.getClaim(toChunk);

        if (fromClaim != null && (!fromClaim.equals(toClaim))) {
            ClaimLeaveEvent leaveEvent = new ClaimLeaveEvent(player, fromClaim);
            Bukkit.getPluginManager().callEvent(leaveEvent);
            LangManager.sendSortedMessage(player, plugin.getLangManager().getString("move.unclaimed_chunk"));
        }

        if (toClaim != null && (fromClaim == null || !fromClaim.equals(toClaim))) {
            ClaimEnterEvent enterEvent = new ClaimEnterEvent(toClaim, player);
            Bukkit.getPluginManager().callEvent(enterEvent);

            boolean isPvpEnabled = NClaim.inst().getClaimSettingsManager().isSettingEnabled(toClaim, Setting.CLAIM_PVP);
            String pvpStatus = plugin.getLangManager().getString(isPvpEnabled ? "move.pvp_enabled" : "move.pvp_disabled");
            OfflinePlayer owner = Bukkit.getOfflinePlayer(toClaim.getOwner());
            LangManager.sendSortedMessage(player, plugin.getLangManager().getString("move.claimed_chunk")
                    .replace("{owner}", owner.getName())
                    .replace("{pvp_status}", pvpStatus));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Entity damager = event.getDamager();
        Location damageLocation = event.getEntity().getLocation();
        Claim claim = Claim.getClaim(damageLocation.getChunk());

        if (claim != null) {
            if ((damager instanceof TNTPrimed || damager instanceof ExplosiveMinecart) &&
                    !plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.TNT_DAMAGE)) {
                event.setCancelled(true);
            }
            else if (damager instanceof Creeper &&
                    !plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.CREEPER_DAMAGE)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getDamager().hasPermission("nclaim.bypass.*") || event.getDamager().hasPermission("nclaim.bypass.pvp")) return;

        Player damaged = (Player) event.getEntity();
        Player damager;

        if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            damager = (Player) ((Projectile) event.getDamager()).getShooter();
        } else if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else {
            return;
        }

        Claim damagedClaim = Claim.getClaim(damaged.getLocation().getChunk());
        Claim damagerClaim = Claim.getClaim(damager.getLocation().getChunk());

        if (damagedClaim != null && !plugin.getClaimSettingsManager().isSettingEnabled(damagedClaim, Setting.CLAIM_PVP)) {
            event.setCancelled(true);
            return;
        }

        if (damagerClaim != null && !plugin.getClaimSettingsManager().isSettingEnabled(damagerClaim, Setting.CLAIM_PVP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location explodeLocation = event.getLocation();
        Claim explodeClaim = Claim.getClaim(explodeLocation.getChunk());

        boolean isTNT = event.getEntityType() == EntityType.TNT ||
                event.getEntityType() == EntityType.TNT_MINECART;
        boolean isCreeper = event.getEntityType() == EntityType.CREEPER;

        List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : event.blockList()) {
            Claim blockClaim = Claim.getClaim(block.getChunk());

            if (blockClaim != null) {
                if (isTNT && !plugin.getClaimSettingsManager().isSettingEnabled(blockClaim, Setting.TNT_DAMAGE)) {
                    blocksToRemove.add(block);
                }
                else if (isCreeper && !plugin.getClaimSettingsManager().isSettingEnabled(blockClaim, Setting.CREEPER_DAMAGE)) {
                    blocksToRemove.add(block);
                }
            }
        }

        event.blockList().removeAll(blocksToRemove);

        if (explodeClaim != null) {
            if ((isTNT && !plugin.getClaimSettingsManager().isSettingEnabled(explodeClaim, Setting.TNT_DAMAGE)) ||
                    (isCreeper && !plugin.getClaimSettingsManager().isSettingEnabled(explodeClaim, Setting.CREEPER_DAMAGE))) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());

        if (claim != null) {
            if (block.getType() == claim.getClaimBlockType()) {
                if (block.getLocation().equals(claim.getClaimBlockLocation())) {
                    if (!player.isSneaking()) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.break")) return;

        if (claim != null) {
            if (block.getType() == Material.SPAWNER) {
                if (!coopManager.hasPermission(player, claim, Permission.BREAK_SPAWNER)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                    return;
                }
            } else {
                if (!coopManager.hasPermission(player, claim, Permission.BREAK_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                }
            }
        }


    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.place")) return;

        if (claim != null) {
            if (block.getType() == Material.SPAWNER) {
                if (!coopManager.hasPermission(player, claim, Permission.PLACE_SPAWNER)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                }
            } else {
                if (!coopManager.hasPermission(player, claim, Permission.PLACE_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        Claim claim = Claim.getClaim(block.getChunk());
        if (claim == null) return;

        // New claim block type check
        if (block.getType() == claim.getClaimBlockType() && claim.getClaimBlockLocation().equals(block.getLocation())) {
            if (coopManager.isClaimOwner(claim, player)) {
                if (player.isSneaking() && player.hasPermission("nclaim.admin")) {
                    new AdminClaimManagementMenu(player, claim);
                } else {
                    new ClaimManagementMenu(player, claim);
                }
            } else if (player.hasPermission("nclaim.admin")) {
                new AdminClaimManagementMenu(player, claim);
            }
            return;
        }

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        Material type = block.getType();

        if (type == Material.CHEST) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_CHEST)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.TRAPPED_CHEST) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_TRAPPED_CHEST)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.BARREL) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BARREL)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.SHULKER_BOXES.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_SHULKER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.HOPPER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_HOPPER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.DISPENSER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_DISPENSER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.DROPPER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_DROPPER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.REPEATER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_REPEATER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.COMPARATOR) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_COMPARATOR)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.BUTTONS.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BUTTONS)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.PRESSURE_PLATES.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_PRESSURE_PLATES)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.LEVER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_LEVERS)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.DOORS.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_DOORS)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.TRAPDOORS.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_TRAPDOORS)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.FENCE_GATES.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_GATES)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.CRAFTING_TABLE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_CRAFTING)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.ENCHANTING_TABLE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_ENCHANTING)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.ANVIL.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_ANVIL)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.GRINDSTONE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_GRINDSTONE)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.STONECUTTER) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_STONECUTTER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.LOOM) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_LOOM)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.SMITHING_TABLE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_SMITHING)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.CARTOGRAPHY_TABLE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_CARTOGRAPHY)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.BREWING_STAND) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BREWING)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.BELL) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BELL)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.BEACON) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BEACON)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.JUKEBOX) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_JUKEBOX)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.NOTE_BLOCK) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_NOTEBLOCK)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.CAMPFIRE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_CAMPFIRE)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (type == Material.SOUL_CAMPFIRE) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_SOUL_CAMPFIRE)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }

        if (Tag.BEDS.isTagged(type)) {
            if (!coopManager.hasPermission(player, claim, Permission.USE_BED)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Claim claim = Claim.getClaim(entity.getLocation().getChunk());

        if (claim == null) return;
        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (entity instanceof Villager) {
            if (!coopManager.hasPermission(player, claim, Permission.INTERACT_VILLAGER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            }
        }
        
        if (entity instanceof ItemFrame) {
            if (!coopManager.hasPermission(player, claim, Permission.INTERACT_ITEM_FRAME)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            }
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        ArmorStand armorStand = event.getRightClicked();
        Claim claim = Claim.getClaim(armorStand.getLocation().getChunk());

        if(player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (claim != null && !coopManager.hasPermission(player, claim, Permission.INTERACT_ARMOR_STAND)) {
            event.setCancelled(true);
            sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        Entity entity = event.getEntered();
        if (!(entity instanceof Player)) return;
        
        Player player = (Player) entity;
        Vehicle vehicle = event.getVehicle();
        Claim claim = Claim.getClaim(vehicle.getLocation().getChunk());

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (claim != null && !coopManager.hasPermission(player, claim, Permission.RIDE_ENTITIES)) {
            event.setCancelled(true);
            sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
        }
    }

    @EventHandler
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        Claim claim = Claim.getClaim(entity.getLocation().getChunk());

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (claim != null && !coopManager.hasPermission(player, claim, Permission.LEASH_MOBS)) {
            event.setCancelled(true);
            sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
        }
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (claim != null) {
            if (event.getBucket() == Material.WATER_BUCKET && !coopManager.hasPermission(player, claim, Permission.TAKE_WATER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            } else if (event.getBucket() == Material.LAVA_BUCKET && !coopManager.hasPermission(player, claim, Permission.TAKE_LAVA)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            }
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());

        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.interact")) return;

        if (claim != null) {
            if (event.getBucket() == Material.WATER_BUCKET && !coopManager.hasPermission(player, claim, Permission.PLACE_WATER)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            } else if (event.getBucket() == Material.LAVA_BUCKET && !coopManager.hasPermission(player, claim, Permission.PLACE_LAVA)) {
                event.setCancelled(true);
                sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();

        if (fromBlock.getType() != Material.WATER && fromBlock.getType() != Material.LAVA) {
            return;
        }

        Claim fromClaim = Claim.getClaim(fromBlock.getChunk());
        Claim toClaim = Claim.getClaim(toBlock.getChunk());

        if (fromClaim == null && toClaim != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedstoneUpdate(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Claim sourceClaim = Claim.getClaim(block.getChunk());
        
        for (BlockFace face : BlockFace.values()) {
            Block relative = block.getRelative(face);
            Claim targetClaim = Claim.getClaim(relative.getChunk());

            if (targetClaim != null && sourceClaim != null) {
                if (!targetClaim.getOwner().equals(sourceClaim.getOwner()) && 
                    !targetClaim.getCoopPlayers().contains(sourceClaim.getOwner())) {
                    event.setNewCurrent(event.getOldCurrent());
                    return;
                }
            }
        }
    }
}