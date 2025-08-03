package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.*;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nandayo.dapi.util.Util;
import org.nandayo.dapi.message.ChannelType;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.api.handlers.FarmerBoughtEvent;
import xyz.geik.farmer.api.managers.FarmerManager;
import xyz.geik.farmer.model.Farmer;
import xyz.geik.farmer.model.user.FarmerPerm;
import xyz.geik.farmer.model.user.User;

import java.util.Optional;
import java.util.UUID;

public class FarmerListener implements Listener {

    @EventHandler
    public void buyClaim(ClaimCreateEvent event) {
        String claimId = event.getClaim().getClaimId();
        if(Main.getConfigFile().getSettings().isAutoCreateFarmer()) {
            new Farmer("nclaim_" + claimId, 0, event.getSender().getUniqueId());
            ChannelType.CHAT.send(event.getSender(), Main.getLangFile().getMessages().getBoughtFarmer());
        }
    }

    @EventHandler
    public void addCoop(ClaimCoopAddEvent event) {
        Claim claim = event.getClaim();
        if (!FarmerManager.getFarmers().containsKey(claim.getClaimId())) return;
        Player coopPlayer = event.getCoopPlayer();
        FarmerManager.getFarmers().get(claim.getClaimId()).addUser(coopPlayer.getUniqueId(), coopPlayer.getName(), FarmerPerm.COOP);
    }

    @EventHandler
    public void kickCoop(ClaimCoopRemoveEvent event) {
        Claim claim = event.getClaim();
        if (!FarmerManager.getFarmers().containsKey(claim.getClaimId())) return;
        UUID coopPlayer = event.getCoopPlayerUUID();
        Optional<User> user = FarmerManager.getFarmers().get(claim.getClaimId()).getUsers().stream().filter(u -> u.getUuid().equals(coopPlayer)).findFirst();
        user.ifPresent(FarmerManager.getFarmers().get(claim.getClaimId())::removeUser);
    }

    @EventHandler
    public void removeClaim(ClaimRemoveEvent event) {
        Claim claim = event.getClaim();
        if (!FarmerManager.getFarmers().containsKey(claim.getClaimId())) return;
        FarmerManager.getFarmers().remove(claim.getClaimId());
    }

    @EventHandler
    public void buyFarmer(FarmerBoughtEvent event) {
        String farmerRegionId = event.getFarmer().getRegionID();

        Optional<Claim> claim = Claim.getClaims().stream()
                .filter(c -> c.getClaimId().equals(farmerRegionId))
                .findFirst();

        if (!claim.isPresent()) return;

        for (UUID coopPlayer : claim.get().getCoopPlayers()) {
            String playerName = NClaim.inst().getServer().getOfflinePlayer(coopPlayer).getName();
            event.getFarmer().addUser(coopPlayer, playerName, FarmerPerm.COOP);
        }
    }
}
