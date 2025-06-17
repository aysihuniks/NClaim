package nesoi.aysihuniks.nclaim.model;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimRemoveEvent;
import nesoi.aysihuniks.nclaim.enums.HoloEnum;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.ui.claim.ClaimListMenu;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.object.DParticle;

import java.util.*;

@Getter
@Setter
public class Claim {

    private NClaim plugin;

    public Claim(
            @NotNull String claimId,
            @NotNull Chunk chunk,
            @NotNull Date createdAt,
            @NotNull Date expiredAt,
            @NotNull UUID owner,
            @NotNull Location bedrockLocation,
            long claimValue,
            Collection<String> lands,
            Collection<UUID> coopPlayers,
            HashMap<UUID, Date> coopPlayerJoinDate,
            HashMap<UUID, CoopPermission> coopPermissions,
            ClaimSetting settings
            ) {
        this.plugin = NClaim.inst();
        this.claimId = claimId;
        this.chunk = chunk;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.owner = owner;
        this.bedrockLocation = bedrockLocation;
        this.claimValue = claimValue;
        this.lands = lands;
        this.coopPlayers = coopPlayers;
        this.coopPlayerJoinDate = coopPlayerJoinDate;
        this.coopPermissions = coopPermissions;
        this.settings = settings;

        claims.removeIf(c -> c.getClaimId().equals(claimId));
        claims.add(this);
    }

    private final @NotNull String claimId;
    private final @NotNull Chunk chunk;
    private final @NotNull Date createdAt;
    private @NotNull Date expiredAt;
    private final @NotNull UUID owner;
    private final @NotNull Location bedrockLocation;
    private long claimValue;
    private final Collection<String> lands;
    private final Collection<UUID> coopPlayers;
    private final HashMap<UUID, Date> coopPlayerJoinDate;

    private final HashMap<UUID, CoopPermission> coopPermissions;
    private final ClaimSetting settings;

    public Collection<Chunk> getAllChunks() {
        List<Chunk> chunks = new ArrayList<>();
        chunks.add(chunk);
        getLands().forEach(l -> chunks.add(NClaim.deserializeChunk(l)));
        return chunks;
    }

    @Getter
    static public Collection<Claim> claims = new ArrayList<>();

    static public Claim getClaim(@NotNull Chunk chunk) {
        return claims.stream()
                .filter(c -> c.getChunk().equals(chunk) || c.getLands().contains(chunk.getWorld().getName() +  "," + chunk.getX() + "," + chunk.getZ()))
                .findFirst().orElse(null);
    }

    public void remove(RemoveCause cause) {
        ClaimRemoveEvent removeEvent = new ClaimRemoveEvent(this, cause);
        Bukkit.getPluginManager().callEvent(removeEvent);

        if (removeEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLangManager().getString("claim.remove_cancelled"));
            }
            return;
        }

        // Cache locations and objects to prevent multiple calls
        World world = getChunk().getWorld();
        Location bedrock = getBedrockLocation();

        bedrock.getBlock().setType(Material.AIR);

        // Remove hologram
        String hologramId = "claim_" + world.getName() + "_" + getChunk().getX() + "_" + getChunk().getZ();

        if (HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM) {
            Hologram hologram = DHAPI.getHologram(hologramId);
            if (hologram != null) {
                hologram.delete();
            }
        } else {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            manager.getHologram(hologramId).ifPresent(manager::removeHologram);
        }

        // Calculate center of chunk
        int centerX = getChunk().getX() * 16 + 8;
        int centerZ = getChunk().getZ() * 16 + 8;

        // Notify online players and play effects
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(plugin.getLangManager().getString("claim.expired")
                    .replace("{x}", String.valueOf(centerX))
                    .replace("{z}", String.valueOf(centerZ)));

            // Calculate and play sound with distance-based volume
            double distance = player.getLocation().distance(bedrock);
            float volume = (float) Math.max(0.2, 1 - (distance / 16.0));
            world.playSound(bedrock, Sound.ENTITY_GENERIC_EXPLODE, volume, 1);
        }

        // Remove claim from the user's claims list
        User.getUser(getOwner()).getPlayerClaims().remove(this);
        User.saveUser(getOwner());

        // Remove claim from coop players
        getCoopPlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> {
                    User user = User.getUser(player.getUniqueId());
                    user.getCoopClaims().remove(this);
                    User.saveUser(player.getUniqueId());
                });

        // Visual effects
        world.spawnParticle(plugin.getParticle(DParticle.LARGE_SMOKE, DParticle.SMOKE_LARGE), bedrock, 1);
        world.playSound(bedrock, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            if ("mysql".equals(plugin.getNconfig().getDatabaseType())) {
                plugin.getMySQLManager().deleteClaim(getClaimId());
            } else {
                plugin.getSqLiteManager().deleteClaim(getClaimId());
            }
        }

        claims.remove(this);
    }
}