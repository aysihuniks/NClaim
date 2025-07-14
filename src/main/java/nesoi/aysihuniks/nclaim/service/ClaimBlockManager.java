package nesoi.aysihuniks.nclaim.service;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nandayo.dapi.Util;

import java.io.File;
import java.util.*;

public class ClaimBlockManager {

    private final Map<Material, ClaimBlockInfo> claimBlocks = new LinkedHashMap<>();

    public ClaimBlockManager() {
        loadClaimBlocks();
    }

    private void loadClaimBlocks() {
        File claimBlocksFile = new File(NClaim.inst().getDataFolder(), "claim_blocks.yml");
        if (!claimBlocksFile.exists()) {
            NClaim.inst().saveResource("claim_blocks.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(claimBlocksFile);

        claimBlocks.clear();

        ConfigurationSection section = config.getConfigurationSection("claim_blocks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String materialKey = key.endsWith("_BLOCK") ? key : key + "_BLOCK";
                Material mat = Material.matchMaterial(materialKey);

                if (mat == null || !mat.isBlock()) {
                    Util.log("Invalid claim block material in config: " + key);
                    Util.log("Material names must end with '_BLOCK' and be valid block materials!");
                    continue;
                }

                ConfigurationSection entry = section.getConfigurationSection(key);
                String displayName = entry.getString("display_name", mat.name());
                double price = entry.getDouble("price", 0);
                List<String> lore = entry.getStringList("lore");
                String permission = entry.getString("permission", null);
                boolean enabled = entry.getBoolean("enabled", true);

                claimBlocks.put(mat, new ClaimBlockInfo(mat, displayName, price, lore, permission, enabled));
            }
        }
    }


    public List<Material> getAllowedBlocks() {
        List<Material> list = new ArrayList<>();
        for (ClaimBlockInfo info : claimBlocks.values()) {
            if (info.enabled) list.add(info.material);
        }
        return list;
    }

    public ClaimBlockInfo getBlockInfo(Material mat) {
        return claimBlocks.get(mat);
    }

    public Collection<ClaimBlockInfo> getAllBlockInfos() {
        return claimBlocks.values();
    }

    public static class ClaimBlockInfo {
        public final Material material;
        public final String displayName;
        public final double price;
        public final List<String> lore;
        public final String permission;
        public final boolean enabled;

        public ClaimBlockInfo(Material material, String displayName, double price, List<String> lore, String permission, boolean enabled) {
            this.material = material;
            this.displayName = displayName;
            this.price = price;
            this.lore = lore;
            this.permission = permission;
            this.enabled = enabled;
        }
    }
}