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
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.object.DEntityType;

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

    private boolean hasClaimBypass(Player player, String specificBypass) {
        return player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass." + specificBypass);
    }

    private boolean isClaimMember(Player player, Claim claim) {
        if (claim == null) return true;
        UUID uuid = player.getUniqueId();
        return !claim.getOwner().equals(uuid) && !claim.getCoopPlayers().contains(uuid);
    }

    private boolean cancelIfNotClaimMember(Player player, Claim claim, Cancellable event) {
        if (claim == null) return false;
        if (hasClaimBypass(player, "")) return false;
        if (isClaimMember(player, claim)) {
            event.setCancelled(true);
            sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
            return true;
        }
        return false;
    }

    private void cancelIfNoPermission(Player player, Claim claim, Permission permission, Cancellable event, String bypassType) {
        if (claim == null) return;
        if (hasClaimBypass(player, bypassType)) return;
        if (!coopManager.hasPermission(player, claim, permission)) {
            event.setCancelled(true);
            sendCooldownMessage(player, plugin.getLangManager().getString("command.permission_denied"));
        }
    }

    private Player getPlayerFromEntity(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        } else if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        if (event.getTo() == null) return;
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
                    .replace("{owner}", owner.getName() != null ? owner.getName() : "Unknown")
                    .replace("{pvp_status}", pvpStatus));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        Location explodeLocation = event.getLocation();
        Claim explodeClaim = Claim.getClaim(explodeLocation.getChunk());
        Entity explodingEntity = event.getEntity();
        boolean isTNT = explodingEntity.getType() == NClaim.getEntityType(DEntityType.TNT, DEntityType.PRIMED_TNT) ||
                explodingEntity.getType() == NClaim.getEntityType(DEntityType.MINECART_TNT, DEntityType.TNT_MINECART);
        boolean isCreeper = explodingEntity.getType() == EntityType.CREEPER;
        List<Block> blocksToRemove = new ArrayList<>();
        for (Block block : event.blockList()) {
            Claim blockClaim = Claim.getClaim(block.getChunk());
            if (blockClaim != null) {
                if ((isTNT && !plugin.getClaimSettingsManager().isSettingEnabled(blockClaim, Setting.TNT_DAMAGE)) ||
                        (isCreeper && !plugin.getClaimSettingsManager().isSettingEnabled(blockClaim, Setting.CREEPER_DAMAGE))) {
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
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        Location location = event.getTo();
        if (location == null) return;
        Chunk chunk = location.getChunk();
        Claim targetClaim = Claim.getClaim(chunk);
        cancelIfNotClaimMember(player, targetClaim, event);
        cancelIfNoPermission(player, targetClaim, Permission.USE_ENDER_PEARL, event, "teleport");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        Hanging hanging = event.getEntity();
        if (!(hanging instanceof ItemFrame || hanging instanceof Painting)) return;
        Player player = getPlayerFromEntity(remover);
        if (player == null) return;
        Claim claim = Claim.getClaim(hanging.getLocation().getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission perm = hanging instanceof Painting ? Permission.PLACE_BLOCKS : Permission.INTERACT_ITEM_FRAME;
        cancelIfNoPermission(player, claim, perm, event, "interact");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Player damager = getPlayerFromEntity(event.getDamager());
            if (damager != null && !hasClaimBypass(damager, "pvp")) {
                Claim damagedClaim = Claim.getClaim(damaged.getLocation().getChunk());
                Claim damagerClaim = Claim.getClaim(damager.getLocation().getChunk());
                if ((damagedClaim != null && !plugin.getClaimSettingsManager().isSettingEnabled(damagedClaim, Setting.CLAIM_PVP)) ||
                        (damagerClaim != null && !plugin.getClaimSettingsManager().isSettingEnabled(damagerClaim, Setting.CLAIM_PVP))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting || event.getEntity() instanceof ArmorStand) {
            Player player = getPlayerFromEntity(event.getDamager());
            if (player == null) return;
            Claim claim = Claim.getClaim(event.getEntity().getLocation().getChunk());
            if (cancelIfNotClaimMember(player, claim, event)) return;
            Permission perm = Permission.INTERACT_ITEM_FRAME;
            if (event.getEntity() instanceof Painting) perm = Permission.PLACE_BLOCKS;
            else if (event.getEntity() instanceof ArmorStand) perm = Permission.INTERACT_ARMOR_STAND;
            cancelIfNoPermission(player, claim, perm, event, "interact");
            return;
        }
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Monster) {
            Player player = (Player) event.getEntity();
            Claim claim = Claim.getClaim(player.getLocation().getChunk());
            if (claim != null) {
                boolean mobAttackingEnabled = plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.MOB_ATTACKING);
                if (!mobAttackingEnabled && isClaimMember(player, claim)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof Monster && event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Claim claim = Claim.getClaim(event.getEntity().getLocation().getChunk());
            if (claim != null && !hasClaimBypass(damager, "mob_attacking")) {
                boolean mobAttackingEnabled = plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.MOB_ATTACKING);
                if (!mobAttackingEnabled && isClaimMember(damager, claim)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof Villager) {
            Player damager = getPlayerFromEntity(event.getDamager());
            if (damager == null) return;
            Claim claim = Claim.getClaim(event.getEntity().getLocation().getChunk());
            if (cancelIfNotClaimMember(damager, claim, event)) return;
            cancelIfNoPermission(damager, claim, Permission.INTERACT_VILLAGER, event, "interact");
            return;
        }
        if (event.getEntity() instanceof LivingEntity) {
            Entity damager = event.getDamager();
            Claim claim = Claim.getClaim(event.getEntity().getLocation().getChunk());
            if (claim != null) {
                if ((damager instanceof TNTPrimed || damager instanceof ExplosiveMinecart) &&
                        !plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.TNT_DAMAGE)) {
                    event.setCancelled(true);
                } else if (damager instanceof Creeper &&
                        !plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.CREEPER_DAMAGE)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void windCharge(ExplosionPrimeEvent event) {
        EntityType type = event.getEntity().getType();
        if (type == EntityType.WIND_CHARGE || type == EntityType.BREEZE_WIND_CHARGE) {
            Claim claim = Claim.getClaim(event.getEntity().getLocation().getChunk());
            if (claim != null && event.getEntity() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getEntity();
                if (projectile.getShooter() instanceof Player) {
                    Player player = (Player) projectile.getShooter();
                    if (cancelIfNotClaimMember(player, claim, event)) return;
                    cancelIfNoPermission(player, claim, Permission.PLACE_BLOCKS, event, "place");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.getType().name().equalsIgnoreCase("WIND_CHARGE")) return;
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null || !(projectile.getShooter() instanceof Player)) return;
        Player player = (Player) projectile.getShooter();
        Claim claim = Claim.getClaim(hitBlock.getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Material type = hitBlock.getType();
        if (Tag.BUTTONS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) ||
                Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
            cancelIfNoPermission(player, claim, Permission.USE_DOORS, event, "interact");
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Claim claim = Claim.getClaim(event.getLocation().getChunk());
        if (claim == null) return;
        if (event.getEntity() instanceof Monster) {
            if (!plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.MONSTER_SPAWNING)) {
                event.setCancelled(true);
            }
        } else {
            if (!plugin.getClaimSettingsManager().isSettingEnabled(claim, Setting.ANIMAL_SPAWNING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());
        if (claim != null && block.getType() == claim.getClaimBlockType() &&
                block.getLocation().equals(claim.getClaimBlockLocation()) && !player.isSneaking()) {
            event.setCancelled(true);
            return;
        }
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = Permission.BREAK_BLOCKS;
        if (block.getType() == Material.SPAWNER) permission = Permission.BREAK_SPAWNER;
        else if (block.getType() == Material.ITEM_FRAME || block.getType() == Material.GLOW_ITEM_FRAME) permission = Permission.INTERACT_ITEM_FRAME;
        else if (block.getType() == Material.PAINTING) permission = Permission.PLACE_BLOCKS;
        cancelIfNoPermission(player, claim, permission, event, "break");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = block.getType() == Material.SPAWNER ? Permission.PLACE_SPAWNER : Permission.PLACE_BLOCKS;
        cancelIfNoPermission(player, claim, permission, event, "place");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;
        Claim claim = Claim.getClaim(block.getChunk());
        if (claim == null) return;

        if (block.getType() == claim.getClaimBlockType() && claim.getClaimBlockLocation().equals(block.getLocation())) {
            if (player.hasPermission("nclaim.admin") && player.isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                new ClaimManagementMenu(player, claim, true);
            }
            else if ((player.hasPermission("nclaim.admin") ||
                    coopManager.isClaimOwner(claim, player) ||
                    coopManager.hasPermission(player, claim, Permission.OPEN_CLAIM_MENU))
                    && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                new ClaimManagementMenu(player, claim, false);
            }
            return;
        }
        if (hasClaimBypass(player, "interact")) return;
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = getInteractPermission(block.getType());
        if (permission == null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Material handType = player.getInventory().getItemInMainHand().getType();
                if (handType == Material.ITEM_FRAME || handType == Material.GLOW_ITEM_FRAME) {
                    permission = Permission.INTERACT_ITEM_FRAME;
                } else if (handType == Material.PAINTING) {
                    permission = Permission.PLACE_BLOCKS;
                } else if (handType == Material.ARMOR_STAND) {
                    permission = Permission.INTERACT_ARMOR_STAND;
                }
            }
            if (permission == null) return;
        }
        cancelIfNoPermission(player, claim, permission, event, "interact");
    }

    private Permission getInteractPermission(Material type) {
        if (type == Material.SPAWNER) return Permission.PLACE_SPAWNER;
        if (type == Material.CHEST) return Permission.USE_CHEST;
        if (type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER) return Permission.USE_FURNACE;
        if (type == Material.TRAPPED_CHEST) return Permission.USE_TRAPPED_CHEST;
        if (type == Material.SWEET_BERRY_BUSH) return Permission.PLACE_BLOCKS;
        if (type == Material.BARREL) return Permission.USE_BARREL;
        if (Tag.SHULKER_BOXES.isTagged(type)) return Permission.USE_SHULKER;
        if (type == Material.HOPPER) return Permission.USE_HOPPER;
        if (type == Material.DISPENSER) return Permission.USE_DISPENSER;
        if (type == Material.DROPPER) return Permission.USE_DROPPER;
        if (type == Material.REPEATER) return Permission.USE_REPEATER;
        if (type == Material.COMPARATOR) return Permission.USE_COMPARATOR;
        if (Tag.BUTTONS.isTagged(type)) return Permission.USE_BUTTONS;
        if (Tag.PRESSURE_PLATES.isTagged(type)) return Permission.USE_PRESSURE_PLATES;
        if (type == Material.LEVER) return Permission.USE_LEVERS;
        if (Tag.DOORS.isTagged(type)) return Permission.USE_DOORS;
        if (Tag.TRAPDOORS.isTagged(type)) return Permission.USE_TRAPDOORS;
        if (Tag.FENCE_GATES.isTagged(type)) return Permission.USE_GATES;
        if (type == Material.CRAFTING_TABLE) return Permission.USE_CRAFTING;
        if (type == Material.ENCHANTING_TABLE) return Permission.USE_ENCHANTING;
        if (Tag.ANVIL.isTagged(type)) return Permission.USE_ANVIL;
        if (type == Material.GRINDSTONE) return Permission.USE_GRINDSTONE;
        if (type == Material.STONECUTTER) return Permission.USE_STONECUTTER;
        if (type == Material.LOOM) return Permission.USE_LOOM;
        if (type == Material.SMITHING_TABLE) return Permission.USE_SMITHING;
        if (type == Material.CARTOGRAPHY_TABLE) return Permission.USE_CARTOGRAPHY;
        if (type == Material.BREWING_STAND) return Permission.USE_BREWING;
        if (type == Material.BELL) return Permission.USE_BELL;
        if (type == Material.BEACON) return Permission.USE_BEACON;
        if (type == Material.JUKEBOX) return Permission.USE_JUKEBOX;
        if (type == Material.NOTE_BLOCK) return Permission.USE_NOTEBLOCK;
        if (type == Material.CAMPFIRE) return Permission.USE_CAMPFIRE;
        if (type == Material.SOUL_CAMPFIRE) return Permission.USE_SOUL_CAMPFIRE;
        if (Tag.BEDS.isTagged(type)) return Permission.USE_BED;
        return null;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Claim claim = Claim.getClaim(entity.getLocation().getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = null;
        if (entity instanceof Villager) permission = Permission.INTERACT_VILLAGER;
        else if (entity instanceof ItemFrame) permission = Permission.INTERACT_ITEM_FRAME;
        else if (player.getInventory().getItemInMainHand().getType() == Material.SHEARS && entity instanceof Shearable) {
            permission = Permission.BREAK_BLOCKS;
        }
        if (permission != null) {
            cancelIfNoPermission(player, claim, permission, event, "interact");
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ArmorStand armorStand = event.getRightClicked();
        Claim claim = Claim.getClaim(armorStand.getLocation().getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        cancelIfNoPermission(player, claim, Permission.INTERACT_ARMOR_STAND, event, "interact");
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;
        Player player = (Player) event.getEntered();
        Vehicle vehicle = event.getVehicle();
        Claim claim = Claim.getClaim(vehicle.getLocation().getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        cancelIfNoPermission(player, claim, Permission.RIDE_ENTITIES, event, "interact");
    }

    @EventHandler
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        Claim claim = Claim.getClaim(entity.getLocation().getChunk());
        if (cancelIfNotClaimMember(player, claim, event)) return;
        cancelIfNoPermission(player, claim, Permission.LEASH_MOBS, event, "interact");
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());
        if (hasClaimBypass(player, "interact")) return;
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = null;
        if (event.getBucket() == Material.WATER_BUCKET) permission = Permission.TAKE_WATER;
        else if (event.getBucket() == Material.LAVA_BUCKET) permission = Permission.TAKE_LAVA;
        if (permission != null) {
            cancelIfNoPermission(player, claim, permission, event, "interact");
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Claim claim = Claim.getClaim(block.getChunk());
        if (hasClaimBypass(player, "interact")) return;
        if (cancelIfNotClaimMember(player, claim, event)) return;
        Permission permission = null;
        if (event.getBucket() == Material.WATER_BUCKET) permission = Permission.PLACE_WATER;
        else if (event.getBucket() == Material.LAVA_BUCKET) permission = Permission.PLACE_LAVA;
        if (permission != null) {
            cancelIfNoPermission(player, claim, permission, event, "interact");
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.isCancelled()) return;
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
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Claim claim = Claim.getClaim(event.getBlock().getChunk());
            if (cancelIfNotClaimMember(player, claim, event)) return;
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