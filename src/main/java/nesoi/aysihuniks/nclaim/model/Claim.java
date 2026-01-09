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
import nesoi.aysihuniks.nclaim.service.ClaimSettingsManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.object.DParticle;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
public class Claim {

    private NClaim plugin;

    public Claim(
            @NotNull String claimId,
            @Nullable String displayName,
            @Nullable String slug,
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
            Set<Material> purchasedBlockTypes,
            boolean forSale,
            double salePrice
    ) {
        this.plugin = NClaim.inst();
        this.claimId = claimId;
        this.displayName = displayName;
        this.slug = (slug == null || slug.isEmpty()) ? toSlug(getDisplayName()) : toSlug(slug);
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
        this.forSale = forSale;
        this.salePrice = salePrice;

        claims.removeIf(c -> c.getClaimId().equals(claimId));
        claims.add(this);
    }

    private final @NotNull String claimId;
    private @Nullable String displayName;
    private @NotNull String slug;
    private final @NotNull Chunk chunk;
    private final @NotNull Date createdAt;
    private @NotNull Date expiredAt;
    private @NotNull UUID owner;
    private @NotNull Location claimBlockLocation;
    private long claimValue;
    private Material claimBlockType;
    private final Collection<String> lands;
    private final Collection<UUID> coopPlayers;
    private final HashMap<UUID, Date> coopPlayerJoinDate;

    private final HashMap<UUID, CoopPermission> coopPermissions;
    private final ClaimSetting settings;
    private final Set<Material> purchasedBlockTypes = new HashSet<>();

    private boolean forSale;
    private double salePrice;

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

        if (NClaim.inst().getHologramManager() != null) {
            if (HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM) {
                Hologram hologram = DHAPI.getHologram(hologramId);
                if (hologram != null) {
                    hologram.delete();
                }
            } else {
                HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
                manager.getHologram(hologramId).ifPresent(manager::removeHologram);
            }
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

        world.spawnParticle(NClaim.getParticle(DParticle.LARGE_SMOKE, DParticle.SMOKE_LARGE), claimBlock, 1);
        world.playSound(claimBlock, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().deleteClaim(getClaimId());
        }

        claims.remove(this);

        if (NClaim.inst().getNconfig().isWebhookEnabled()
                && NClaim.inst().getNconfig().getWebhookUrl() != null
                && !NClaim.inst().getNconfig().getWebhookUrl().isEmpty()) {
            try {
                String ownerName = Bukkit.getOfflinePlayer(getOwner()).getName();
                String worldName = getChunk().getWorld().getName();
                int x = getChunk().getX() * 16 + 8;
                int y = getClaimBlockLocation().getBlockY();
                int z = getChunk().getZ() * 16 + 8;

                String payload;
                if (NClaim.inst().getNconfig().isWebhookUseEmbed()) {
                    String description = getString(ownerName);

                    StringBuilder json = new StringBuilder();
                    json.append("{");

                    if (NClaim.inst().getNconfig().getWebhookMention() != null && !NClaim.inst().getNconfig().getWebhookMention().isEmpty()) {
                        json.append("\"content\":\"").append(NClaim.inst().getNconfig().getWebhookMention()).append("\",");
                    }

                    json.append("\"embeds\":[{");
                    if (!NClaim.inst().getNconfig().getWebhookEmbedTitle().isEmpty()) {
                        json.append("\"title\":\"").append(escapeJson(NClaim.inst().getNconfig().getWebhookEmbedTitle())).append("\",");
                    }
                    json.append("\"description\":\"").append(escapeJson(description)).append("\",");
                    json.append("\"color\":").append(Integer.parseInt(NClaim.inst().getNconfig().getWebhookEmbedColor().replace("#", ""), 16)).append(",");
                    if (!NClaim.inst().getNconfig().getWebhookEmbedFooter().isEmpty()) {
                        json.append("\"footer\":{\"text\":\"").append(escapeJson(NClaim.inst().getNconfig().getWebhookEmbedFooter())).append("\"},");
                    }
                    if (NClaim.inst().getNconfig().isWebhookEmbedTimestamp()) {
                        json.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\",");
                    }
                    if (!NClaim.inst().getNconfig().getWebhookEmbedImage().isEmpty()) {
                        json.append("\"image\":{\"url\":\"").append(escapeJson(NClaim.inst().getNconfig().getWebhookEmbedImage())).append("\"},");
                    }
                    if (!NClaim.inst().getNconfig().getWebhookEmbedThumbnail().isEmpty()) {
                        json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(NClaim.inst().getNconfig().getWebhookEmbedThumbnail())).append("\"},");
                    }
                    if (json.charAt(json.length() - 1) == ',') {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}]}");
                    payload = json.toString();
                } else {
                    String content = NClaim.inst().getNconfig().getWebhookContent()
                            .replace("%player%", ownerName != null ? ownerName : "Unknown")
                            .replace("%world%", worldName)
                            .replace("%x%", String.valueOf(x))
                            .replace("%y%", String.valueOf(y))
                            .replace("%z%", String.valueOf(z));
                    StringBuilder json = new StringBuilder();
                    json.append("{");
                    if (NClaim.inst().getNconfig().getWebhookMention() != null && !NClaim.inst().getNconfig().getWebhookMention().isEmpty()) {
                        json.append("\"content\":\"").append(NClaim.inst().getNconfig().getWebhookMention()).append(" ");
                    } else {
                        json.append("\"content\":\"");
                    }
                    json.append(escapeJson(content)).append("\"}");
                    payload = json.toString();
                }

                URL url = new URL(NClaim.inst().getNconfig().getWebhookUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                byte[] out = payload.getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(out);

                int responseCode = connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        }
    }

    private @NotNull String getString(String ownerName) {
        String worldName = getChunk().getWorld().getName();
        int x = getChunk().getX() * 16 + 8;
        int y = getClaimBlockLocation().getBlockY();
        int z = getChunk().getZ() * 16 + 8;

        return NClaim.inst().getNconfig().getWebhookEmbedDescription()
                .replace("%player%", ownerName != null ? ownerName : "Unknown")
                .replace("%world%", worldName)
                .replace("%x%", String.valueOf(x))
                .replace("%y%", String.valueOf(y))
                .replace("%z%", String.valueOf(z));
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

    public Optional<Player> getOwnerPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(owner));
    }

    @ApiStatus.Internal
    public void moveClaimBlock(@NotNull Location newLocation) {
        Block oldBlock = claimBlockLocation.getBlock();
        BlockData oldBlockData = oldBlock.getBlockData().clone();
        oldBlock.setType(Material.AIR);

        this.claimBlockLocation = newLocation;
        Block block = claimBlockLocation.getBlock();
        block.setBlockData(oldBlockData, false);

        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createHologram(claimBlockLocation);
        }
    }

    public boolean isSafeToTeleport() {
        return claimBlockLocation.clone().add(0,1,0).getBlock().getType().isAir() &&
                claimBlockLocation.clone().add(0,2,0).getBlock().getType().isAir();
    }

    public void teleport(Player teleporter) {
        teleporter.teleport(claimBlockLocation.clone().add(0.5,1,0.5));
    }

    public String getDisplayName() {
        return displayName != null ? displayName : claimId;
    }

    public static String toSlug(@NotNull String input) {
        String s = input.trim().toLowerCase(java.util.Locale.ROOT);

        s = s.replace("ı", "i").replace("ğ", "g").replace("ü", "u")
                .replace("ş", "s").replace("ö", "o").replace("ç", "c");
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        s = s.replaceAll("-{2,}", "-");

        if (s.isEmpty()) {
            s = "claim";
        }

        if (s.length() > 24) s = s.substring(0, 24);
        s = s.replaceAll("^-+|-+$", "");
        if (s.isEmpty()) s = "claim";

        return s;
    }

    public static Claim getClaimByIdentifier(UUID owner, String identifier) {
        if (owner == null || identifier == null) return null;
        String id = identifier.trim();
        if (id.isEmpty()) return null;

        Claim bySlug = claims.stream()
                .filter(c -> owner.equals(c.getOwner()))
                .filter(c -> c.getSlug() != null && c.getSlug().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
        if (bySlug != null) return bySlug;

        return claims.stream()
                .filter(c -> owner.equals(c.getOwner()))
                .filter(c -> c.getClaimId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public static Claim getClaimByName(UUID owner, String name) {
        if (owner == null || name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;

        return claims.stream()
                .filter(c -> owner.equals(c.getOwner()))
                .filter(c -> c.getDisplayName() != null && c.getDisplayName().equalsIgnoreCase(n))
                .findFirst()
                .orElse(null);
    }

    public void sellTheClaim(@NotNull Player player) {
        UUID oldOwner = this.owner;
        UUID newOwner = player.getUniqueId();

        setOwner(newOwner);

        this.forSale = false;

        ClaimSettingsManager.resetToDefaults(this);

        this.coopPlayers.clear();
        this.coopPlayerJoinDate.clear();
        this.coopPermissions.clear();

        NClaim.inst().getClaimStorageManager().saveClaim(this);

        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.sell.bought_buyer").replace("{display_slug}", getDisplayName()).replace("{player}", player.getName()).replace("{price}", String.valueOf(getSalePrice())));
        Player seller = Bukkit.getPlayer(oldOwner);
        if (seller != null) {
            ChannelType.CHAT.send(seller, NClaim.inst().getLangManager().getString("claim.sell.bought_seller").replace("{player}", player.getName()).replace("{display_slug}", getDisplayName()).replace("{price}", String.valueOf(getSalePrice())));
        }
    }
}