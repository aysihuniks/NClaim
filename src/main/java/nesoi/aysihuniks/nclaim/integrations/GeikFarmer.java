package nesoi.aysihuniks.nclaim.integrations;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.FarmerListener;
import org.bukkit.Location;
import org.nandayo.dapi.util.Util;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.integrations.Integrations;

import java.util.UUID;

public class GeikFarmer extends Integrations {

    public GeikFarmer() {
        super(new FarmerListener());
    }

    @Override
    public UUID getOwnerUUID(String regionId) {
        for (Claim claims : Claim.getClaims()) {
            if (claims.getClaimId().equals(regionId)) {
                return claims.getOwner();
            }
        }
        return null;
    }

    @Override
    public UUID getOwnerUUID(Location location) {
        Claim claim = Claim.getClaim(location.getChunk());
        return claim != null ? claim.getOwner() : null;
    }

    @Override
    public String getRegionID(Location location) {
        Claim claim = Claim.getClaim(location.getChunk());
        return claim != null ? claim.getClaimId() : null;
    }


    public static void registerIntegration() {
        if (NClaim.inst().getServer().getPluginManager().getPlugin("Farmer") != null) {
            NClaim.inst().getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(() -> {
                try {
                    Main.setIntegration(new GeikFarmer());
                    Util.log("&aFarmer integration enabled successfully!");
                } catch (Exception e) {
                    Util.log("&cFailed to initialize Farmer integration: " + e.getMessage());
                }
            }, 1L);
        } else {
            Util.log("&eFarmer plugin not found! Farmer integration disabled.");
        }
    }
}
