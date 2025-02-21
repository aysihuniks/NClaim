
package nesoi.network.NClaim.menus.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.coop.ManageCoopsMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.Material;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.GUIManager.Button;
import org.nandayo.DAPI.GUIManager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.nandayo.DAPI.HexUtil.parse;

public class ClaimSettingsMenu extends Menu {

    private final Chunk chunk;

    public ClaimSettingsMenu(Player p, Chunk chunk) {
        this.createInventory(9 * 6, "NClaim - Manage Settings");
        this.chunk = chunk;
        setup();
        displayTo(p);
    }

    void setup() {
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;

        List<String> settings = Arrays.asList(
                "claim-pvp",
                "tnt-damage",
                "creeper-damage",
                "mob-attacking",
                "monsters-spawning",
                "animals-spawning",
                "villager-interaction"
        );

        List<String> settingNames = Arrays.asList(
                "PvP",
                "TNT Explosions",
                "Creeper Explosions",
                "Mob Attacks",
                "Monster Spawning",
                "Animal Spawning",
                "Villager Interactions"
        );

        List<List<String>> settingLores = Arrays.asList(
                Arrays.asList("", "{GRAY}Allow players to {WHITE}fight each other", "{GRAY}inside the claim?", ""), // PvP = default false
                Arrays.asList("", "{GRAY}Allow {WHITE}TNT explosions", "{GRAY}to break blocks inside the claim?", ""), // TNT Explosions = default true
                Arrays.asList("", "{GRAY}Allow {WHITE}Creeper explosions", "{GRAY}to break blocks inside the claim?", ""), // Creeper Explosions = default true
                Arrays.asList("", "{GRAY}Allow players to {WHITE}attack mobs", "{GRAY}inside the claim?", ""), // Mob Attacks = default false
                Arrays.asList("", "{GRAY}Allow {WHITE}monsters to spawn", "{GRAY}inside the claim?", ""), // Monster Spawning = default true
                Arrays.asList("", "{GRAY}Allow {WHITE}animals to spawn", "{GRAY}inside the claim?", ""), // Animal Spawning = default true
                Arrays.asList("", "{GRAY}Allow players to {WHITE}interact with villagers", "{GRAY}inside the claim?", "") // Villager Interactions = default false
        );

        List<Material> materials = Arrays.asList(
                Material.DIAMOND_SWORD,
                Material.TNT,
                Material.CREEPER_SPAWN_EGG,
                Material.GOLDEN_APPLE,
                Material.ZOMBIE_SPAWN_EGG,
                Material.COW_SPAWN_EGG,
                Material.VILLAGER_SPAWN_EGG
        );

        this.addButton(new Button(45) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{ORANGE}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ClaimMenu(p, chunk);
            }
        });

        List<Integer> itemSlots = Arrays.asList(11, 20, 29, 38, 14, 23, 32);
        List<Integer> dyeSlots = Arrays.asList(12, 21, 30, 39, 15, 24, 33);

        for (int i = 0; i < settings.size(); i++) {
            String setting = settings.get(i);
            String settingName = settingNames.get(i);
            List<String> originalSettingLore = settingLores.get(i);
            Material material = materials.get(i);

            List<String> settingLore = new ArrayList<>(originalSettingLore);

            boolean defaultValue = switch (setting) {
                case "tnt-damage", "creeper-damage", "monsters-spawning", "animals-spawning" -> true;
                default -> false;
            };

            boolean settingStatus = claimDataManager.isClaimSettingEnabled(chunk, setting, defaultValue);
            String settingColor = settingStatus ? "{GREEN}Enabled" : "{RED}Disabled";
            Material toggleMaterial = settingStatus ? Material.LIME_DYE : Material.GRAY_DYE;

            settingLore.add("{GRAY}Status: " + parse(settingColor));

            this.addButton(new Button(itemSlots.get(i)) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(material)
                            .name("{BROWN}" + settingName)
                            .lore(settingLore)
                            .hideFlag(ItemFlag.values())
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    claimDataManager.toggleClaimSetting(chunk, setting);
                    new ClaimSettingsMenu(p, chunk);
                }
            });

            this.addButton(new Button(dyeSlots.get(i)) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(toggleMaterial)
                            .name(" ")
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    claimDataManager.toggleClaimSetting(chunk, setting);
                    new ClaimSettingsMenu(p, chunk);
                }

            });
        }
    }
}