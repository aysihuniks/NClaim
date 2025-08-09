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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;

@RequiredArgsConstructor
public class ClaimCoopManager {
    private final NClaim plugin;

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

    public void addCoopPlayer(Claim claim, Player owner, Player coopPlayer) {
        if (!canAddCoop(claim, owner, coopPlayer)) {
            return;
        }

        UUID coopUUID = coopPlayer.getUniqueId();
        ClaimCoopAddEvent addEvent = new ClaimCoopAddEvent(owner, coopPlayer, claim);
        Bukkit.getPluginManager().callEvent(addEvent);

        if (addEvent.isCancelled()) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.add_cancelled"));
            return;
        }

        addCoopToClaimData(claim, coopUUID);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
        }

        ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.added")
                .replace("{coop}", coopPlayer.getName()));
        ChannelType.CHAT.send(coopPlayer, plugin.getLangManager().getString("claim.coop.joined")
                .replace("{owner}", owner.getName()));
    }

    public void removeCoopPlayer(Claim claim, Player owner, UUID coopUUID) {
        if (!isClaimOwner(claim, owner)) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("command.permission_denied"));
            return;
        }

        ClaimCoopRemoveEvent removeEvent = new ClaimCoopRemoveEvent(owner, coopUUID, claim);
        Bukkit.getPluginManager().callEvent(removeEvent);

        if (removeEvent.isCancelled()) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.remove_cancelled"));
            return;
        }

        removeCoopFromClaimData(claim, coopUUID);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
        }

        Player coopPlayer = Bukkit.getPlayer(coopUUID);
        String coopName = coopPlayer != null ? coopPlayer.getName() : coopUUID.toString();
        if (coopPlayer != null) {
            ChannelType.CHAT.send(coopPlayer, plugin.getLangManager().getString("claim.coop.kicked")
                    .replace("{owner}", owner.getName()));
        }
        ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.removed")
                .replace("{coop}", coopName));
    }

    public void toggleCoopPermission(@NotNull Claim claim, @NotNull UUID player, @NotNull Permission permission) {
        boolean newState = !claim.getCoopPermissions().get(player).isEnabled(permission);
        ClaimCoopPermissionToggleEvent toggleEvent = new ClaimCoopPermissionToggleEvent(
                player,
                claim,
                permission,
                newState
        );
        Bukkit.getPluginManager().callEvent(toggleEvent);

        if (toggleEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(claim.getOwner());
            if (owner != null) {
                ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.permission_toggle_cancelled"));
            }
            return;
        }

        claim.getCoopPermissions().get(player).toggle(permission);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
        }
    }

    public void toggleCoopPermissionCategory(Claim claim, @NotNull UUID player, @NotNull PermissionCategory category) {
        boolean currentState = claim.getCoopPermissions().get(player).hasAllPermissionsInCategory(category);
        boolean newState = !currentState;

        ClaimCoopPermissionCategoryToggleEvent toggleEvent = new ClaimCoopPermissionCategoryToggleEvent(
                player,
                claim,
                category,
                newState
        );
        Bukkit.getPluginManager().callEvent(toggleEvent);

        if (toggleEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(claim.getOwner());
            if (owner != null) {
                ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.permission_category_toggle_cancelled"));
            }
            return;
        }

        claim.getCoopPermissions().get(player).setAllPermissionsInCategory(category, newState);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
        }
    }

    private void addCoopToClaimData(Claim claim, UUID coopUUID) {
        claim.getCoopPlayers().add(coopUUID);
        claim.getCoopPlayerJoinDate().put(coopUUID, new Date());
        claim.getCoopPermissions().put(coopUUID, new CoopPermission());

        User.getUser(coopUUID).getCoopClaims().add(claim);
        User.saveUser(coopUUID);
    }

    private void removeCoopFromClaimData(Claim claim, UUID coopUUID) {
        claim.getCoopPlayers().remove(coopUUID);
        claim.getCoopPlayerJoinDate().remove(coopUUID);
        claim.getCoopPermissions().remove(coopUUID);

        User.getUser(coopUUID).getCoopClaims().remove(claim);
        User.saveUser(coopUUID);
    }

    private boolean canAddCoop(Claim claim, Player owner, Player coopPlayer) {
        if (!isClaimOwner(claim, owner) && plugin.getClaimCoopManager().hasPermission(coopPlayer, claim, Permission.ADD_COOP)) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("command.permission_denied"));
            return false;
        }

        UUID coopUUID = coopPlayer.getUniqueId();
        if (claim.getOwner().equals(coopUUID)) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("command.player.cant_add_self"));
            return false;
        }

        if (isCoopPlayer(claim, coopUUID)) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.already_added")
                    .replace("{coop}", coopPlayer.getName()));
            return false;
        }

        if (claim.getCoopPlayers().size() >= plugin.getNconfig().getMaxCoopPlayers(owner)) {
            ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.coop.limit_reached"));
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