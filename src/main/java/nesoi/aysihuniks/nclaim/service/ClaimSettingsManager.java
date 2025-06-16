package nesoi.aysihuniks.nclaim.service;

import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimSettingChangeEvent;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.model.CoopPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
public class ClaimSettingsManager {
    private final NClaim plugin;

    public void toggleSetting(Claim claim, Player player, Setting setting) {
        if (!isAuthorized(claim, player)) {
            player.sendMessage(plugin.getLangManager().getString("command.permission_denied"));
            return;
        }

        boolean newState = !claim.getSettings().isEnabled(setting);

        ClaimSettingChangeEvent changeEvent = new ClaimSettingChangeEvent(claim, claim.getSettings(), player, newState);
        Bukkit.getPluginManager().callEvent(changeEvent);

        if (changeEvent.isCancelled()) {
            player.sendMessage(plugin.getLangManager().getString("claim.setting_change_cancelled"));
            return;
        }

        claim.getSettings().set(setting, newState);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getMySQLManager().saveClaim(claim);
        }
    }

    public boolean isSettingEnabled(Claim claim, Setting setting) {
        return claim.getSettings().isEnabled(setting);
    }

    private boolean isAuthorized(Claim claim, Player player) {
        return claim.getOwner().equals(player.getUniqueId()) || 
               player.hasPermission("nclaim.admin");
    }
}