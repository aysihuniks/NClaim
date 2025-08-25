package nesoi.aysihuniks.nclaim.service;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimBuyLandEvent;
import nesoi.aysihuniks.nclaim.api.events.ClaimCreateEvent;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.model.SettingCfg;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.enums.Balance;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.Economy;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.util.Util;

import java.util.*;

@RequiredArgsConstructor
public class ClaimService {
    private final NClaim plugin;

    public void buyNewClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        User user = User.getUser(player.getUniqueId());

        if (!canCreateClaim(player, user, chunk)) {
            return;
        }

        if (!handleClaimPayment(player, user)) {
            return;
        }

        createNewClaim(player, chunk);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(Claim.getClaim(chunk));
            plugin.getDatabaseManager().saveUser(user);
        }
    }

    public void buyLand(@NotNull Claim claim, Player player, @NotNull Chunk chunk) {
        if (claim.getChunk().equals(chunk)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.land.already_own_chunk"));
            return;
        }

        String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
        if (claim.getLands().contains(chunkKey)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.land.already_own_land"));
            return;
        }

        if (!isAdjacentChunk(claim, chunk)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.land.not_adjacent"));
            return;
        }

        if (Claim.getClaim(chunk) != null) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.already_claimed"));
            return;
        }

        if (!canClaimOrExpandNearOthers(player, chunk)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.too_close_to_other_claim"));
            return;
        }

        User user = User.getUser(player.getUniqueId());
        double landPrice = calculateLandPrice(claim);

        ClaimBuyLandEvent buyLandEvent = new ClaimBuyLandEvent(player, claim, chunk);
        Bukkit.getPluginManager().callEvent(buyLandEvent);

        if (buyLandEvent.isCancelled()) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.land.buy_cancelled"));
            return;
        }

        if (!player.hasPermission("nclaim.bypass.*") && !player.hasPermission("nclaim.bypass.land_buy_price")) {
            if (!handlePayment(player, user, landPrice)) {
                return;
            }
        }

        claim.getLands().add(chunkKey);

        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
            plugin.getDatabaseManager().saveUser(user);
        }

        ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.land.expanded"));
    }

    public double calculateLandPrice(@NotNull Claim claim) {
        int currentChunkCount = 1 + claim.getLands().size();
        int nextChunkNumber = currentChunkCount + 1;

        if (nextChunkNumber > 41) {
            return -1;
        }

        return plugin.getNconfig().getTieredPrice(nextChunkNumber);
    }

    private boolean isAdjacentChunk(Claim claim, Chunk targetChunk) {
        int claimX = claim.getChunk().getX();
        int claimZ = claim.getChunk().getZ();
        int targetX = targetChunk.getX();
        int targetZ = targetChunk.getZ();

        if (isAdjacent(claimX, claimZ, targetX, targetZ)) {
            return true;
        }

        for (String landKey : claim.getLands()) {
            String[] coords = landKey.split(",");
            if (coords.length >= 3) {
                int landX = Integer.parseInt(coords[1]);
                int landZ = Integer.parseInt(coords[2]);
                if (isAdjacent(landX, landZ, targetX, targetZ)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isAdjacent(int x1, int z1, int x2, int z2) {
        return (Math.abs(x1 - x2) == 1 && z1 == z2) || (x1 == x2 && Math.abs(z1 - z2) == 1);
    }

    private boolean canCreateClaim(Player player, User user, Chunk chunk) {
        if (NClaim.inst().getNconfig().getBlacklistedWorlds().contains(chunk.getWorld().getName())) {
            if (!player.hasPermission("nclaim.bypass.*") && !player.hasPermission("nclaim.bypass.blacklisted_worlds")) {
                ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.in_blacklisted_region_or_world"));
                return false;
            }
        }

        if (isInBlacklistedRegion(player.getLocation())) {
            if (!player.hasPermission("nclaim.bypass.*") && !player.hasPermission("nclaim.bypass.blacklisted_regions")) {
                ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.in_blacklisted_region_or_world"));
                return false;
            }
        }

        if (Claim.getClaim(chunk) != null) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.already_claimed"));
            return false;
        }

        if (!player.hasPermission("nclaim.buy")) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("command.permission_denied"));
            return false;
        }

        if (user.getPlayerClaims().size() >= plugin.getNconfig().getMaxClaimCount(player)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.max_reached"));
            return false;
        }

        if (!canClaimOrExpandNearOthers(player, chunk)) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.too_close_to_other_claim"));
            return false;
        }

        return true;
    }

    private boolean canClaimOrExpandNearOthers(Player player, Chunk chunk) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();


        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                if (x == chunkX && z == chunkZ) {
                    continue;
                }

                Chunk nearbyChunk = chunk.getWorld().getChunkAt(x, z);
                Claim nearbyClaim = Claim.getClaim(nearbyChunk);

                if (nearbyClaim != null) {
                    if (nearbyClaim.getCoopPlayers().contains(player.getUniqueId())) continue;

                    if (!nearbyClaim.getOwner().equals(player.getUniqueId())) {
                        ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.too_close_to_other_claim"));
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean handleClaimPayment(Player player, User user) {
        if (player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.claim_buy_price")) {
            return true;
        }

        double claimPrice = plugin.getNconfig().getClaimBuyPrice();
        return handlePayment(player, user, claimPrice);
    }

    private void createNewClaim(Player player, Chunk chunk) {
        String claimId = generateClaimId(chunk);
        Date createdAt = new Date();
        Date expiredAt = calculateExpirationDate();
        Location claimBlockLocation = player.getLocation().getBlock().getLocation();

        Material defaultBlockType = NClaim.inst().getNconfig().getDefaultClaimBlockType();
        claimBlockLocation.getBlock().setType(defaultBlockType);

        long initialValue = plugin.getBlockValueManager().calculateChunkValue(chunk);

        Set<Material> purchasedBlockTypes = new HashSet<>();
        purchasedBlockTypes.add(defaultBlockType);

        ClaimSetting claimSetting = new ClaimSetting();
        for (Setting setting : Setting.values()) {
            SettingCfg cfg = plugin.getGuiLangManager().getSettingConfig(setting);
            boolean defaultValue = cfg != null ? cfg.isDefaultValue() : claimSetting.isEnabled(setting);
            claimSetting.set(setting, defaultValue);
        }

        Claim claim = new Claim(
                claimId,
                chunk,
                createdAt,
                expiredAt,
                player.getUniqueId(),
                claimBlockLocation,
                initialValue,
                defaultBlockType,
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>(),
                claimSetting,
                purchasedBlockTypes
        );
        ClaimCreateEvent createEvent = new ClaimCreateEvent(player, claim);
        Bukkit.getPluginManager().callEvent(createEvent);

        if (createEvent.isCancelled()) {
            ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.buy_cancelled"));
            return;
        }

        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createHologram(claimBlockLocation);
        }
        User.getUser(player.getUniqueId()).getPlayerClaims().add(claim);
        ChannelType.CHAT.send(player, plugin.getLangManager().getString("claim.received"));
    }

    private String generateClaimId(Chunk chunk) {
        return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }

    private Date calculateExpirationDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, plugin.getNconfig().getClaimExpiryDays());
        return calendar.getTime();
    }

    private boolean handlePayment(Player player, User user, double amount) {
        if (plugin.getBalanceSystem() == Balance.PLAYERDATA) {
            if (user.getBalance() >= amount) {
                user.setBalance(user.getBalance() - amount);
                return true;
            }
        } else if (plugin.getBalanceSystem() == Balance.VAULT) {
            Economy econ = plugin.getEconomy();
            if (econ != null && econ.has(player, amount)) {
                econ.withdrawPlayer(player, amount);
                return true;
            }
        }

        ChannelType.CHAT.send(player, plugin.getLangManager().getString("command.balance.not_enough"));
        return false;
    }

    private boolean isInBlacklistedRegion(Location location) {

        if (!NClaim.inst().isWorldGuardEnabled()) return false;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (container == null) return false;

            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));

            List<String> blacklistedRegions = plugin.getNconfig().getBlacklistedRegions();
            if (blacklistedRegions == null || blacklistedRegions.isEmpty()) return false;

            for (ProtectedRegion region : set) {
                if (blacklistedRegions.contains(region.getId().toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Util.log("&cFailed to check if location is in blacklisted region: " + e.getMessage());
            return false;
        }

        return false;
    }
}