
package nesoi.network.NClaim.menus.claim.inside;

import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.ClaimSetting;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.nandayo.DAPI.HexUtil.parse;

public class SettingMenu extends Menu {

    private final Claim claim;

    public SettingMenu(Player p, Claim claim) {
        createInventory(9 * 6, "NClaim - Manage Settings");
        this.claim = claim;
        setup();
        displayTo(p);
    }

    void setup() {
        List<ClaimSetting.Setting> settings = Arrays.asList(
                ClaimSetting.Setting.CLAIM_PVP,
                ClaimSetting.Setting.TNT_DAMAGE,
                ClaimSetting.Setting.CREEPER_DAMAGE,
                ClaimSetting.Setting.MOB_ATTACKING,
                ClaimSetting.Setting.MONSTER_SPAWNING,
                ClaimSetting.Setting.ANIMAL_SPAWNING,
                ClaimSetting.Setting.VILLAGER_INTERACTION
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

        addButton(new Button(45) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{ORANGE}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ClaimMenu(p, claim);
            }
        });

        List<Integer> itemSlots = Arrays.asList(11, 20, 29, 38, 14, 23, 32);
        List<Integer> dyeSlots = Arrays.asList(12, 21, 30, 39, 15, 24, 33);

        for (int i = 0; i < settings.size(); i++) {
            ClaimSetting.Setting settingEnum = settings.get(i);
            String settingName = settingNames.get(i);
            List<String> originalSettingLore = settingLores.get(i);
            Material material = materials.get(i);

            List<String> settingLore = new ArrayList<>(originalSettingLore);
            boolean settingStatus = claim.getSettingState(settingEnum);
            String settingColor = settingStatus ? "{GREEN}Enabled" : "{RED}Disabled";
            Material toggleMaterial = settingStatus ? Material.LIME_DYE : Material.GRAY_DYE;

            settingLore.add("{GRAY}Status: " + parse(settingColor));

            addButton(new Button(itemSlots.get(i)) {
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
                    claim.getSettings().toggle(settingEnum);
                    new SettingMenu(p, claim);
                }
            });

            addButton(new Button(dyeSlots.get(i)) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(toggleMaterial)
                            .name("{BROWN}" + settingName)
                            .lore(settingLore)
                            .hideFlag(ItemFlag.values())
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    claim.toggleClaimSetting(settingEnum);
                    new SettingMenu(p, claim);
                }
            });
        }
    }
}