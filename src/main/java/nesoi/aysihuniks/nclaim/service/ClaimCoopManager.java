package nesoi.aysihuniks.nclaim.service;

import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimCoopAddEvent;
import nesoi.aysihuniks.nclaim.api.events.ClaimCoopPermissionCategoryToggleEvent;
import nesoi.aysihuniks.nclaim.api.events.ClaimCoopRemoveEvent;
import nesoi.aysihuniks.nclaim.api.events.ClaimCoopPermissionToggleEvent;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.PermissionCategory;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.CoopPermission;
import nesoi.aysihuniks.nclaim.model.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@RequiredArgsConstructor
public class ClaimCoopManager {
    private final NClaim plugin;

    private boolean isContainer(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST ||
               type == Material.BARREL || type == Material.SHULKER_BOX ||
               Tag.SHULKER_BOXES.isTagged(type) || type == Material.DISPENSER ||
               type == Material.DROPPER || type == Material.HOPPER ||
               type == Material.FURNACE || type == Material.BLAST_FURNACE ||
               type == Material.SMOKER;
    }

    private boolean isWorkstation(Material type) {
        return type == Material.CRAFTING_TABLE || type == Material.ENCHANTING_TABLE ||
               Tag.ANVIL.isTagged(type) || type == Material.GRINDSTONE ||
               type == Material.STONECUTTER || type == Material.LOOM ||
               type == Material.SMITHING_TABLE || type == Material.CARTOGRAPHY_TABLE ||
               type == Material.BREWING_STAND;
    }

    private boolean isRedstone(Material type) {
        return type == Material.REPEATER || type == Material.COMPARATOR ||
               type == Material.REDSTONE_WIRE || Tag.BUTTONS.isTagged(type) ||
               Tag.PRESSURE_PLATES.isTagged(type) || type == Material.LEVER ||
               type == Material.DAYLIGHT_DETECTOR;
    }

    private boolean isDoor(Material type) {
        return Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) ||
               Tag.FENCE_GATES.isTagged(type);
    }

    public boolean hasPermission(Player player, Claim claim, Permission permission) {
        if (player.hasPermission("nclaim.bypass") || isClaimOwner(claim, player)) {
            return true;
        }
        
        UUID playerUuid = player.getUniqueId();
        if (!claim.getCoopPlayers().contains(playerUuid)) {
            return false;
        }
        
        return claim.getCoopPermissions().get(playerUuid).isEnabled(permission);
    }

    public boolean canModifyBlock(Player player, Claim claim, Block block, boolean isBreaking) {
        Material type = block.getType();
        
        if (type == Material.SPAWNER) {
            return hasPermission(player, claim, isBreaking ? 
                    Permission.BREAK_SPAWNER : 
                    Permission.PLACE_SPAWNER);
        }
        
        return hasPermission(player, claim, isBreaking ? 
                Permission.BREAK_BLOCKS : 
                Permission.PLACE_BLOCKS);
    }

    public boolean canUseContainer(Player player, Claim claim, Material type) {
        Permission permission = null;

        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
            permission = Permission.USE_CHEST;
        } else if (type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER) {
            permission = Permission.USE_FURNACE;
        } else if (type == Material.BARREL) {
            permission = Permission.USE_BARREL;
        } else if (type == Material.SHULKER_BOX) {
            permission = Permission.USE_SHULKER;
        } else if (type == Material.HOPPER) {
            permission = Permission.USE_HOPPER;
        } else if (type == Material.DISPENSER || type == Material.DROPPER) {
            permission = Permission.USE_DISPENSER;
        }

        return permission != null && hasPermission(player, claim, permission);
    }

    public boolean canUseInteractable(Player player, Claim claim, Material type) {
        if (isWorkstation(type)) {
            Permission permission = null;

            if (type == Material.CRAFTING_TABLE) {
                permission = Permission.USE_CRAFTING;
            } else if (type == Material.ENCHANTING_TABLE) {
                permission = Permission.USE_ENCHANTING;
            } else if (type == Material.ANVIL || type == Material.CHIPPED_ANVIL || type == Material.DAMAGED_ANVIL) {
                permission = Permission.USE_ANVIL;
            } else if (type == Material.GRINDSTONE) {
                permission = Permission.USE_GRINDSTONE;
            } else if (type == Material.STONECUTTER) {
                permission = Permission.USE_STONECUTTER;
            } else if (type == Material.LOOM) {
                permission = Permission.USE_LOOM;
            } else if (type == Material.SMITHING_TABLE) {
                permission = Permission.USE_SMITHING;
            } else if (type == Material.CARTOGRAPHY_TABLE) {
                permission = Permission.USE_CARTOGRAPHY;
            } else if (type == Material.BREWING_STAND) {
                permission = Permission.USE_BREWING;
            }

            return permission != null && hasPermission(player, claim, permission);
        }

        if (isRedstone(type)) {
            Permission permission = null;

            if (type == Material.REPEATER || type == Material.COMPARATOR || type == Material.REDSTONE_WIRE) {
                permission = Permission.USE_REDSTONE;
            } else if (type == Material.STONE_BUTTON || type == Material.OAK_BUTTON || type == Material.BIRCH_BUTTON) {
                permission = Permission.USE_BUTTONS;
            } else if (type == Material.STONE_PRESSURE_PLATE || type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
                permission = Permission.USE_PRESSURE_PLATES;
            } else if (type == Material.LEVER) {
                permission = Permission.USE_LEVERS;
            }

            return permission != null && hasPermission(player, claim, permission);
        }

        if (isDoor(type)) {
            if (Tag.DOORS.isTagged(type)) return hasPermission(player, claim, Permission.USE_DOORS);
            if (Tag.TRAPDOORS.isTagged(type)) return hasPermission(player, claim, Permission.USE_TRAPDOORS);
            if (Tag.FENCE_GATES.isTagged(type)) return hasPermission(player, claim, Permission.USE_GATES);
        }

        Permission permission = null;

        if (type == Material.BELL) {
            permission = Permission.USE_BELL;
        } else if (type == Material.BEACON) {
            permission = Permission.USE_BEACON;
        } else if (type == Material.JUKEBOX) {
            permission = Permission.USE_JUKEBOX;
        } else if (type == Material.NOTE_BLOCK) {
            permission = Permission.USE_NOTEBLOCK;
        } else if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            permission = Permission.USE_CAMPFIRE;
        }

        return permission != null && hasPermission(player, claim, permission);
    }


    public boolean canInteractWithEntity(Player player, Claim claim, Entity entity) {
        if (entity instanceof Villager) {
            return hasPermission(player, claim, Permission.INTERACT_VILLAGER);
        }
        
        if (entity instanceof org.bukkit.entity.ArmorStand) {
            return hasPermission(player, claim, Permission.INTERACT_ARMOR_STAND);
        }

        if (entity instanceof org.bukkit.entity.ItemFrame) {
            return hasPermission(player, claim, Permission.INTERACT_ITEM_FRAME);
        }

        return true;
    }

    public boolean canUseBed(Player player, Claim claim) {
        return hasPermission(player, claim, Permission.USE_BED);
    }

    public boolean canManageLiquids(Player player, Claim claim, boolean isLava) {
        if (isLava) {
            return hasPermission(player, claim, Permission.PLACE_LAVA);
        }
        return hasPermission(player, claim, Permission.PLACE_WATER);
    }

    public void addCoopPlayer(Claim claim, Player owner, Player coopPlayer) {
        if (!canAddCoop(claim, owner, coopPlayer)) {
            return;
        }

        UUID coopUUID = coopPlayer.getUniqueId();
        ClaimCoopAddEvent addEvent = new ClaimCoopAddEvent(owner, coopPlayer, claim);
        Bukkit.getPluginManager().callEvent(addEvent);

        if (addEvent.isCancelled()) {
            owner.sendMessage(plugin.getLangManager().getString("claim.coop.add_cancelled"));
            return;
        }

        addCoopToClaimData(claim, coopUUID);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getMySQLManager().saveClaim(claim);
        }

        owner.sendMessage(plugin.getLangManager().getString("claim.coop.added")
                .replace("{coop}", coopPlayer.getName()));
        coopPlayer.sendMessage(plugin.getLangManager().getString("claim.coop.joined")
                .replace("{owner}", owner.getName()));
    }

    public void removeCoopPlayer(Claim claim, Player owner, UUID coopUUID) {
        if (!isClaimOwner(claim, owner)) {
            owner.sendMessage(plugin.getLangManager().getString("command.permission_denied"));
            return;
        }

        ClaimCoopRemoveEvent removeEvent = new ClaimCoopRemoveEvent(owner, coopUUID, claim);
        Bukkit.getPluginManager().callEvent(removeEvent);

        if (removeEvent.isCancelled()) {
            owner.sendMessage(plugin.getLangManager().getString("claim.coop.remove_cancelled"));
            return;
        }

        removeCoopFromClaimData(claim, coopUUID);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getMySQLManager().saveClaim(claim);
        }

        Player coopPlayer = Bukkit.getPlayer(coopUUID);
        String coopName = coopPlayer != null ? coopPlayer.getName() : coopUUID.toString();
        if (coopPlayer != null) {
            coopPlayer.sendMessage(plugin.getLangManager().getString("claim.coop.kicked")
                    .replace("{owner}", owner.getName()));
        }
        owner.sendMessage(plugin.getLangManager().getString("claim.coop.removed")
                .replace("{coop}", coopName));
    }

    public void toggleCoopPermission(@NotNull Claim claim, @NotNull UUID player, @NotNull Permission permission) {
        boolean newState = !claim.getCoopPermissions().get(player).isEnabled(permission);
        ClaimCoopPermissionToggleEvent toggleEvent = new ClaimCoopPermissionToggleEvent(
                Bukkit.getPlayer(claim.getOwner()),
                player,
                claim,
                permission,
                newState
        );
        Bukkit.getPluginManager().callEvent(toggleEvent);

        if (toggleEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(claim.getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLangManager().getString("claim.coop.permission_toggle_cancelled"));
            }
            return;
        }

        claim.getCoopPermissions().get(player).toggle(permission);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getMySQLManager().saveClaim(claim);
        }
    }

    public void toggleCoopPermissionCategory(Claim claim, @NotNull UUID player, @NotNull PermissionCategory category) {
        boolean currentState = claim.getCoopPermissions().get(player).hasAllPermissionsInCategory(category);
        boolean newState = !currentState;

        ClaimCoopPermissionCategoryToggleEvent toggleEvent = new ClaimCoopPermissionCategoryToggleEvent(
                Bukkit.getPlayer(claim.getOwner()),
                player,
                claim,
                category,
                newState
        );
        Bukkit.getPluginManager().callEvent(toggleEvent);

        if (toggleEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(claim.getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLangManager().getString("claim.coop.permission_category_toggle_cancelled"));
            }
            return;
        }

        claim.getCoopPermissions().get(player).setAllPermissionsInCategory(category, newState);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getMySQLManager().saveClaim(claim);
        }
    }

    private void addCoopToClaimData(Claim claim, UUID coopUUID) {
        claim.getCoopPlayers().add(coopUUID);
        claim.getCoopPlayerJoinDate().put(coopUUID, new Date());
        claim.getCoopPermissions().put(coopUUID, new CoopPermission());
    }

    private void removeCoopFromClaimData(Claim claim, UUID coopUUID) {
        claim.getCoopPlayers().remove(coopUUID);
        claim.getCoopPlayerJoinDate().remove(coopUUID);
        claim.getCoopPermissions().remove(coopUUID);
    }

    private boolean canAddCoop(Claim claim, Player owner, Player coopPlayer) {
        if (!isClaimOwner(claim, owner)) {
            owner.sendMessage(plugin.getLangManager().getString("claim.not_yours"));
            return false;
        }

        UUID coopUUID = coopPlayer.getUniqueId();
        if (claim.getOwner().equals(coopUUID)) {
            owner.sendMessage(plugin.getLangManager().getString("command.player.cant_add_self"));
            return false;
        }

        if (isCoopPlayer(claim, coopUUID)) {
            owner.sendMessage(plugin.getLangManager().getString("claim.coop.already_added")
                    .replace("{coop}", coopPlayer.getName()));
            return false;
        }

        if (claim.getCoopPlayers().size() >= plugin.getNconfig().getMaxCoopPlayers()) {
            owner.sendMessage(plugin.getLangManager().getString("claim.coop.limit_reached"));
            return false;
        }

        return true;
    }

    public boolean isClaimOwner(Claim claim, Player player) {
        return claim.getOwner().equals(player.getUniqueId());
    }

    private boolean isCoopPlayer(Claim claim, UUID playerUUID) {
        return claim.getCoopPlayers().contains(playerUUID);
    }

    public boolean getCoopPermissionState(Claim claim, @NotNull UUID player, @NotNull Permission permission) {
        return claim.getCoopPermissions().get(player).isEnabled(permission);
    }
}