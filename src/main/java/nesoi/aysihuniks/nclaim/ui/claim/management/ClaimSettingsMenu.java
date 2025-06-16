package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.SettingData;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClaimSettingsMenu extends BaseMenu {
    private final Claim claim;
    private final int page;

    private static final int[] settingSlots = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final List<SettingData> settings = Arrays.asList(
            new SettingData(Setting.CLAIM_PVP, "pvp", Material.DIAMOND_SWORD),
            new SettingData(Setting.TNT_DAMAGE, "tnt_explosions", Material.TNT),
            new SettingData(Setting.CREEPER_DAMAGE, "creeper_explosions", Material.CREEPER_SPAWN_EGG),
            new SettingData(Setting.MOB_ATTACKING, "mob_attacks", Material.GOLDEN_APPLE),
            new SettingData(Setting.MONSTER_SPAWNING, "monster_spawning", Material.ZOMBIE_SPAWN_EGG),
            new SettingData(Setting.ANIMAL_SPAWNING, "animal_spawning", Material.COW_SPAWN_EGG),
            new SettingData(Setting.VILLAGER_INTERACTION, "villager_interactions", Material.VILLAGER_SPAWN_EGG)
    );

    public ClaimSettingsMenu(Player player, Claim claim, int page) {
        super("menu.settings_menu");
        this.claim = claim;
        this.page = page;

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);
        addNavigationButton();
        addSettingButtons();
        
        if (hasNextPage()) {
            addNextPageButton();
        }
    }

    private void addNavigationButton() {
        addButton(new Button() {
            final String buttonPath = page == 0 ? "menu.back" : "menu.previous_page";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(page == 0 ? Material.OAK_DOOR : Material.FEATHER)
                        .name(langManager.getString(buttonPath + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (page == 0) {
                    new ClaimManagementMenu(player, claim);
                } else {
                    new ClaimSettingsMenu(player, claim, page - 1);
                }
            }
        });
    }

    private void addNextPageButton() {
        addButton(new Button() {
            final String buttonPath = "menu.next_page";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.COMPASS)
                        .name(langManager.getString(buttonPath + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new ClaimSettingsMenu(player, claim, page + 1);
            }
        });
    }

    private void addSettingButtons() {
        int startIndex = page * settingSlots.length;
        int endIndex = Math.min(startIndex + settingSlots.length, settings.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            SettingData settingData = settings.get(i);
            addSettingButton(settingData, slotIndex);
        }
    }

    private void addSettingButton(SettingData settingData, int slotIndex) {
        addButton(new Button() {
            final String buttonPath = "settings." + settingData.getConfigKey();

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(settingSlots[slotIndex]);
            }

            @Override
            public ItemStack getItem() {
                boolean isEnabled = claim.getSettings().isEnabled(settingData.getSetting());
                String status = isEnabled ? langManager.getString("menu.enabled") : langManager.getString("menu.disabled");
                List<String> lore = getStringList(buttonPath + ".lore");
                lore.replaceAll(l -> l.replace("{status}", status));

                return ItemCreator.of(settingData.getMaterial())
                        .name(getString(buttonPath + ".display_name"))
                        .lore(lore)
                        .hideFlag(ItemFlag.values())
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.CONFIRM.playSound(player);
                NClaim.inst().getClaimSettingsManager().toggleSetting(claim, player, settingData.getSetting());
                new ClaimSettingsMenu(player, claim, page);
            }
        });
    }

    private boolean hasNextPage() {
        return (page + 1) * settingSlots.length < settings.size();
    }
}