package nesoi.aysihuniks.nclaim.service;

import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.model.CoopData;
import nesoi.aysihuniks.nclaim.model.CoopPermission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClaimStorageManager {
    private final NClaim plugin;

    private volatile boolean isLoading = false;

    public void loadClaims() {
        if (isLoading) {
            return;
        }

        isLoading = true;

        try {
            if (plugin.getNconfig().isDatabaseEnabled()) {
                Claim.claims.clear();
                List<Claim> loadedClaims = plugin.getDatabaseManager().loadAllClaims();
                Claim.claims.addAll(loadedClaims);

                for (Claim claim : Claim.claims) {
                    long value = plugin.getBlockValueManager().calculateClaimValue(claim);
                    claim.setClaimValue(value);
                }

                Util.log("&aLoaded " + Claim.claims.size() + " claims from database.");
                return;
            }

            Claim.claims.clear();
            File file = new File(plugin.getDataFolder(), "claims.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection section = config.getConfigurationSection("chunks_claimed");
            if (section == null) return;

            int loadedCount = 0;
            for (String claimId : section.getKeys(false)) {
                Claim claim = loadClaim(section, claimId);
                if (claim != null) {
                    long value = plugin.getBlockValueManager().calculateClaimValue(claim);
                    claim.setClaimValue(value);
                    loadedCount++;
                }
            }

            Util.log("&aLoaded " + loadedCount + " claims from file.");

        } finally {
            isLoading = false;
        }
    }

    public void saveClaims() {
        if (plugin.getNconfig().isDatabaseEnabled()) {
            try {
                plugin.getDatabaseManager().saveClaimsBatch(new ArrayList<>(Claim.claims));
                Util.log("&eSaved " + Claim.claims.size() + " claims to database.");
            } catch (Exception e) {
                Util.log("&cFailed to save claims to database: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        FileConfiguration config = new YamlConfiguration();
        for (Claim claim : Claim.claims) {
            try {
                saveClaim(config, claim);
            } catch (Exception e) {
                Util.log("&cError saving claim " + claim.getClaimId() + ": " + e.getMessage());
            }
        }

        try {
            File file = new File(plugin.getDataFolder(), "claims.yml");
            config.save(file);
        } catch (Exception e) {
            Util.log("&cFailed to save claims.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Claim loadClaim(ConfigurationSection section, String claimId) {
        String[] chunkParts = claimId.split("_");
        if (chunkParts.length != 3) return null;

        World world = Bukkit.getWorld(chunkParts[0]);
        if (world == null) return null;

        try {
            int x = Integer.parseInt(chunkParts[1]);
            int z = Integer.parseInt(chunkParts[2]);

            Date createdAt = NClaim.deserializeDate(section.getString(claimId + ".created_at"));
            Date expiredAt = NClaim.deserializeDate(section.getString(claimId + ".expired_at"));
            UUID owner = UUID.fromString(section.getString(claimId + ".owner", ""));
            Location claimBlockLocation = NClaim.deserializeLocation(section.getString(claimId + ".claim_block_location"));
            long claimValue = section.getLong(claimId + ".value", 0);

            String blockTypeName = section.getString(claimId + ".claim_block_type", "OBSIDIAN");
            Material blockType = Material.matchMaterial(blockTypeName);
            if (blockType == null) blockType = Material.OBSIDIAN;

            if (createdAt == null || expiredAt == null || claimBlockLocation == null) return null;

            Collection<String> lands = section.getStringList(claimId + ".lands");
            CoopData coopData = loadCoopData(section.getConfigurationSection(claimId + ".coops"));
            ClaimSetting settings = loadClaimSettings(section.getConfigurationSection(claimId + ".settings"));

            Set<Material> purchasedBlocks = new HashSet<>();
            List<String> purchasedBlockNames = section.getStringList(claimId + ".purchased_blocks");
            for (String blockName : purchasedBlockNames) {
                try {
                    Material material = Material.valueOf(blockName);
                    purchasedBlocks.add(material);
                } catch (IllegalArgumentException e) {
                    Util.log("&cInvalid material in purchased blocks for claim " + claimId + ": " + blockName);
                }
            }

            return new Claim(claimId,
                    world.getChunkAt(x, z),
                    createdAt,
                    expiredAt,
                    owner,
                    claimBlockLocation,
                    claimValue,
                    blockType,
                    lands,
                    coopData.getCoopPlayers(),
                    coopData.getJoinDates(),
                    coopData.getPermissions(),
                    settings,
                    purchasedBlocks);

        } catch (Exception e) {
            Util.log("&c[ERROR] Exception while loading claim " + claimId + ": " + e.getMessage());
            return null;
        }
    }

    private CoopData loadCoopData(ConfigurationSection coopSection) {
        Collection<UUID> coopPlayers = new ArrayList<>();
        HashMap<UUID, Date> joinDates = new HashMap<>();
        HashMap<UUID, CoopPermission> permissions = new HashMap<>();

        if (coopSection != null) {
            for (String coopPlayer : coopSection.getKeys(false)) {
                UUID coopPlayerUUID = UUID.fromString(coopPlayer);
                coopPlayers.add(coopPlayerUUID);

                Date joinDate = NClaim.deserializeDate(coopSection.getString(coopPlayerUUID + ".joined_at"));
                joinDates.put(coopPlayerUUID, joinDate);

                CoopPermission permission = loadCoopPermissions(
                    coopSection.getConfigurationSection(coopPlayerUUID + ".permissions")
                );
                permissions.put(coopPlayerUUID, permission);
            }
        }

        return new CoopData(coopPlayers, joinDates, permissions);
    }

    private CoopPermission loadCoopPermissions(ConfigurationSection permissionSection) {
        CoopPermission permission = new CoopPermission();
        if (permissionSection != null) {
            for (String permissionName : permissionSection.getKeys(false)) {
                try {
                    Permission perm = Permission.valueOf(permissionName);
                    permission.setEnabled(perm, permissionSection.getBoolean(permissionName));
                } catch (IllegalArgumentException e) {
                    Util.log("&cInvalid permission: " + permissionName);
                }
            }
        }
        return permission;
    }


    private ClaimSetting loadClaimSettings(ConfigurationSection settingSection) {
        ClaimSetting settings = new ClaimSetting();
        if (settingSection != null) {
            for (String settingName : settingSection.getKeys(false)) {
                try {
                    Setting setting = Setting.valueOf(
                        settingName.toUpperCase(Locale.ENGLISH).replace("-", "_")
                    );
                    settings.set(setting, settingSection.getBoolean(settingName));
                } catch (IllegalArgumentException e) {
                    Util.log("&cInvalid setting: " + settingName);
                }
            }
        }
        return settings;
    }

    public void saveClaim(Claim claim) {
        if (plugin.getNconfig().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveClaim(claim);
            return;
        }
        File file = new File(plugin.getDataFolder(), "claims.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        saveClaim(config, claim);

        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveClaim(FileConfiguration config, Claim claim) {
        String ns = "chunks_claimed." + claim.getClaimId();
        config.set(ns + ".created_at", NClaim.serializeDate(claim.getCreatedAt()));
        config.set(ns + ".expired_at", NClaim.serializeDate(claim.getExpiredAt()));
        config.set(ns + ".owner", claim.getOwner().toString());
        config.set(ns + ".claim_block_location", NClaim.serializeLocation(claim.getClaimBlockLocation()));
        config.set(ns + ".lands", claim.getLands());
        config.set(ns + ".value", claim.getClaimValue());
        config.set(ns + ".claim_block_type", claim.getClaimBlockType().name());

        List<String> purchasedBlockNames = claim.getPurchasedBlockTypes().stream()
                .map(Material::name)
                .collect(Collectors.toList());
        config.set(ns + ".purchased_blocks", purchasedBlockNames);

        for (UUID coopPlayerUUID : claim.getCoopPlayers()) {
            String coopPath = ns + ".coops." + coopPlayerUUID;
            config.set(coopPath + ".joined_at",
                    NClaim.serializeDate(claim.getCoopPlayerJoinDate().get(coopPlayerUUID)));

            CoopPermission coopPermission = claim.getCoopPermissions()
                    .getOrDefault(coopPlayerUUID, new CoopPermission());

            for (Permission permission : Permission.values()) {
                config.set(coopPath + ".permissions." + permission,
                        coopPermission.isEnabled(permission));
            }
        }

        for (Setting setting : Setting.values()) {
            config.set(ns + ".settings." + setting,
                    claim.getSettings().isEnabled(setting));
        }
    }
}