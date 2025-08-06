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
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;
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
            @NotNull Location claimBlockLocation,
            long claimValue,
            Material claimBlockType,
            Collection<String> lands,
            Collection<UUID> coopPlayers,
            HashMap<UUID, Date> coopPlayerJoinDate,
            HashMap<UUID, CoopPermission> coopPermissions,
            ClaimSetting settings,
            Set<Material> purchasedBlockTypes
    ) {
        this.plugin = NClaim.inst();
        this.claimId = claimId;
        this.chunk = chunk;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.owner = owner;
        this.claimBlockLocation = claimBlockLocation;
        this.claimBlockType = claimBlockType;
        this.claimValue = claimValue;
        this.lands = lands;
        this.coopPlayers = coopPlayers;
        this.coopPlayerJoinDate = coopPlayerJoinDate;
        this.coopPermissions = coopPermissions;
        this.settings = settings;
        if (purchasedBlockTypes != null) {
            this.purchasedBlockTypes.addAll(purchasedBlockTypes);
        }

        claims.removeIf(c -> c.getClaimId().equals(claimId));
        claims.add(this);
    }

    private final @NotNull String claimId;
    private final @NotNull Chunk chunk;
    private final @NotNull Date createdAt;
    private @NotNull Date expiredAt;
    private @NotNull UUID owner;
    private final @NotNull Location claimBlockLocation;
    private long claimValue;
    private Material claimBlockType;
    private final Collection<String> lands;
    private final Collection<UUID> coopPlayers;
    private final HashMap<UUID, Date> coopPlayerJoinDate;

    private final HashMap<UUID, CoopPermission> coopPermissions;
    private final ClaimSetting settings;
    private final Set<Material> purchasedBlockTypes = new HashSet<>();


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

    private volatile boolean isBeingRemoved = false;

    public void remove(RemoveCause cause) {
        if (isBeingRemoved) {
            return;
        }

        isBeingRemoved = true;

        ClaimRemoveEvent removeEvent = new ClaimRemoveEvent(this, cause);
        Bukkit.getPluginManager().callEvent(removeEvent);

        if (removeEvent.isCancelled()) {
            Player owner = Bukkit.getPlayer(getOwner());
            if (owner != null) {
                ChannelType.CHAT.send(owner, plugin.getLangManager().getString("claim.remove_cancelled"));
            }
            return;
        }

        World world = getChunk().getWorld();
        Location claimBlock = getClaimBlockLocation();

        claimBlock.getBlock().setType(Material.AIR);

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

        int centerX = getChunk().getX() * 16 + 8;
        int centerZ = getChunk().getZ() * 16 + 8;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(claimBlock.getWorld())) {
                double distance = player.getLocation().distance(claimBlock);
                float volume = (float) Math.max(0.2, 1 - (distance / 16.0));
                world.playSound(claimBlock, Sound.ENTITY_GENERIC_EXPLODE, volume, 1);

                ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.expired")
                        .replace("{x}", String.valueOf(centerX))
                        .replace("{z}", String.valueOf(centerZ)));
            }
        }

        User ownerUser = User.getUser(getOwner());
        if (ownerUser == null) {
            User.loadUser(getOwner());
            ownerUser = User.getUser(getOwner());
        }
        if (ownerUser != null) {
            ownerUser.getPlayerClaims().remove(this);
            User.saveUser(getOwner());
        }

        getCoopPlayers().forEach(uuid -> {
            User coopUser = User.getUser(uuid);
            if (coopUser == null) {
                User.loadUser(uuid);
                coopUser = User.getUser(uuid);
            }
            if (coopUser != null) {
                coopUser.getCoopClaims().remove(this);
                User.saveUser(uuid);
            }
        });

        world.spawnParticle(plugin.getParticle(DParticle.LARGE_SMOKE, DParticle.SMOKE_LARGE), claimBlock, 1);
        world.playSound(claimBlock, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().deleteClaim(getClaimId());
        }

        claims.remove(this);
    }

    public void setOwner(@NotNull UUID newOwner) {
        UUID oldOwner = getOwner();
        User oldOwnerUser = User.getUser(oldOwner);
        if (oldOwnerUser == null) {
            User.loadUser(oldOwner);
            oldOwnerUser = User.getUser(oldOwner);
        }

        if (oldOwnerUser != null) {
            oldOwnerUser.getPlayerClaims().remove(this);
            User.saveUser(oldOwner);
        }

        owner = newOwner;

        User newOwnerUser = User.getUser(newOwner);
        if (newOwnerUser == null) {
            User.loadUser(newOwner);
            newOwnerUser = User.getUser(newOwner);
        }

        if (newOwnerUser != null) {
            newOwnerUser.getPlayerClaims().add(this);
            User.saveUser(newOwner);
        }

        getCoopPlayers().remove(newOwner);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(this);
        }
    }

    public boolean isOwner(UUID uuid) {
        return getOwner().equals(uuid);
    }
}