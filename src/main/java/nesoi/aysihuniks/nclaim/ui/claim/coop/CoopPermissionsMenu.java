package nesoi.aysihuniks.nclaim.ui.claim.coop;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.PermissionCategory;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.*;
import java.util.stream.Collectors;

public class CoopPermissionsMenu extends BaseMenu {
    private final @Nullable OfflinePlayer coopPlayer;
    private final @NotNull Claim claim;
    private final boolean admin;
    private final @Nullable PermissionCategory currentCategory;

    private static final int[] CATEGORY_SLOTS = {28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final int[] PERMISSION_SLOTS = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<PermissionCategory, Material> CATEGORY_ICONS;
    static {
        Map<PermissionCategory, Material> map = new HashMap<>();
        map.put(PermissionCategory.BLOCKS, Material.GRASS_BLOCK);
        map.put(PermissionCategory.CONTAINERS, Material.CHEST);
        map.put(PermissionCategory.REDSTONE, Material.REDSTONE);
        map.put(PermissionCategory.DOORS, Material.OAK_DOOR);
        map.put(PermissionCategory.WORKSTATIONS, Material.CRAFTING_TABLE);
        map.put(PermissionCategory.INTERACTIONS, Material.LEVER);
        map.put(PermissionCategory.LIQUIDS, Material.WATER_BUCKET);
        map.put(PermissionCategory.ENTITIES, Material.VILLAGER_SPAWN_EGG);
        CATEGORY_ICONS = Collections.unmodifiableMap(map);
    }


    public CoopPermissionsMenu(@NotNull Player player, @Nullable OfflinePlayer coopPlayer, @NotNull Claim claim, boolean admin, @Nullable PermissionCategory category) {
        super("manage_coop_player_permission_menu");
        this.coopPlayer = coopPlayer;
        this.claim = claim;
        this.admin = admin;
        this.currentCategory = category;

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title").replace("{player}", coopPlayer.getName()));
        setBackgroundButton(BackgroundMenu::getButton);

        addBackButton();
        addPlayerInfoButton();
        addCategoryButtons();
        addPermissionButtons();
        addTransferButton();
        addKickButton();
    }

    private void addBackButton() {

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(currentCategory == null ? Material.OAK_DOOR : Material.FEATHER)
                        .name(NClaim.inst().getGuiLangManager().getString((currentCategory == null ? "back" : "previous_page") + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (currentCategory == null) {
                    new CoopListMenu(player, claim, admin, 0);
                } else {
                    new CoopPermissionsMenu(player,  coopPlayer, claim, admin, null);
                }
            }
        });
    }

    private void addPlayerInfoButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                String playerName = coopPlayer.isOnline() ? 
                    "&a" + coopPlayer.getName() : 
                    "&7" + coopPlayer.getName() + " " + getString("offline");

                List<String> lore = new ArrayList<>(getStringList("player_info.lore"));
                lore.replaceAll(s -> s.replace("{date}", 
                    NClaim.serializeDate(claim.getCoopPlayerJoinDate().get(coopPlayer.getUniqueId()))));

                return ItemCreator.of(NClaim.inst().getHeadManager().createHead(coopPlayer))
                        .name(getString("player_info.display_name").replace("{player}", playerName))
                        .lore(lore)
                        .get();
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void addCategoryButtons() {
        if (currentCategory != null) return;

        int slot = 0;
        for (PermissionCategory category : PermissionCategory.values()) {
            if (slot >= CATEGORY_SLOTS.length) break;
            final int currentSlot = slot++;

            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(CATEGORY_SLOTS[currentSlot]);
                }

                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(CATEGORY_ICONS.get(category))
                            .name(getString("categories." +
                                    category.name().toLowerCase() + ".display_name"))
                            .lore(Arrays.asList(
                                    "",
                                    getString("click_to_view"),
                                    "",
                                    getString("right_click_toggle")
                            ))
                            .get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    if (clickType == ClickType.RIGHT) {
                        MessageType.CONFIRM.playSound(player);
                        NClaim.inst().getClaimCoopManager()
                                .toggleCoopPermissionCategory(claim, coopPlayer.getUniqueId(), category);
                    }
                    MessageType.MENU_FORWARD.playSound(player);
                    new CoopPermissionsMenu(player, coopPlayer, claim, admin, category);
                }
            });
        }
    }


    private void addPermissionButtons() {
        if (currentCategory == null) return;

        Permission[] permissions = claim.getCoopPermissions()
                .get(coopPlayer.getUniqueId())
                .getPermissionsByCategory()
                .get(currentCategory);

        int slot = 0;
        for (Permission permission : permissions) {
            if (slot >= PERMISSION_SLOTS.length) break;
            final int currentSlot = slot++;

            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(PERMISSION_SLOTS[currentSlot]);
                }

                @Override
                public ItemStack getItem() {
                    boolean isEnabled = claim.getCoopPermissions()
                            .get(coopPlayer.getUniqueId())
                            .isEnabled(permission);

                    return ItemCreator.of(getPermissionIcon(permission))
                            .name((isEnabled ? "&a" : "&c") + getString("permissions." +
                                    permission.name().toLowerCase() + ".display_name"))
                            .lore(Arrays.asList(
                                    "",
                                    NClaim.inst().getGuiLangManager().getString(isEnabled ? "enabled.display_name" : "disabled.display_name")
                            ))
                            .hideFlag(ItemFlag.values())
                            .get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    NClaim.inst().getClaimCoopManager()
                            .toggleCoopPermission(claim, coopPlayer.getUniqueId(), permission);
                    MessageType.CONFIRM.playSound(player);
                    new CoopPermissionsMenu(player, coopPlayer, claim, admin, currentCategory);
                }
            });
        }
    }


    private Material getPermissionIcon(Permission permission) {
        switch (permission) {
            case BREAK_BLOCKS:
                return Material.DIAMOND_PICKAXE;
            case BREAK_SPAWNER:
            case PLACE_SPAWNER:
                return Material.SPAWNER;
            case PLACE_BLOCKS:
                return Material.GRASS_BLOCK;
            case USE_CHEST:
                return Material.CHEST;
            case USE_FURNACE:
                return Material.FURNACE;
            case USE_BARREL:
                return Material.BARREL;
            case USE_SHULKER:
                return Material.SHULKER_BOX;
            case USE_HOPPER:
                return Material.HOPPER;
            case USE_DISPENSER:
                return Material.DISPENSER;
            case USE_DROPPER:
                return Material.DROPPER;
            case USE_REPEATER:
                return Material.REPEATER;
            case USE_COMPARATOR:
                return Material.COMPARATOR;
            case USE_BUTTONS:
                return Material.STONE_BUTTON;
            case USE_PRESSURE_PLATES:
                return Material.STONE_PRESSURE_PLATE;
            case USE_LEVERS:
                return Material.LEVER;
            case USE_DOORS:
                return Material.OAK_DOOR;
            case USE_TRAPDOORS:
                return Material.OAK_TRAPDOOR;
            case USE_GATES:
                return Material.OAK_FENCE_GATE;
            case USE_CRAFTING:
                return Material.CRAFTING_TABLE;
            case USE_ENCHANTING:
                return Material.ENCHANTING_TABLE;
            case USE_ANVIL:
                return Material.ANVIL;
            case USE_GRINDSTONE:
                return Material.GRINDSTONE;
            case USE_STONECUTTER:
                return Material.STONECUTTER;
            case USE_LOOM:
                return Material.LOOM;
            case USE_SMITHING:
                return Material.SMITHING_TABLE;
            case USE_CARTOGRAPHY:
                return Material.CARTOGRAPHY_TABLE;
            case USE_BREWING:
                return Material.BREWING_STAND;
            case USE_BELL:
                return Material.BELL;
            case USE_BEACON:
                return Material.BEACON;
            case USE_JUKEBOX:
                return Material.JUKEBOX;
            case USE_NOTEBLOCK:
                return Material.NOTE_BLOCK;
            case USE_CAMPFIRE:
                return Material.CAMPFIRE;
            case USE_BED:
                return Material.RED_BED;
            case INTERACT_ARMOR_STAND:
                return Material.ARMOR_STAND;
            case INTERACT_ITEM_FRAME:
                return Material.ITEM_FRAME;
            case PLACE_WATER:
                return Material.WATER_BUCKET;
            case PLACE_LAVA:
                return Material.LAVA_BUCKET;
            case TAKE_WATER:
            case TAKE_LAVA:
                return Material.BUCKET;
            case INTERACT_VILLAGER:
                return Material.EMERALD;
            case LEASH_MOBS:
                return Material.LEAD;
            case RIDE_ENTITIES:
                return Material.SADDLE;
            default:
                return Material.BARRIER;
        }
    }

    private void addTransferButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GOLDEN_HELMET)
                        .name(getString("transfer.display_name"))
                        .lore(getStringList("transfer.lore").stream().map(line -> line.replace("{player}", coopPlayer.getName())).collect(Collectors.toList()))
                        .hideFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (coopPlayer == null) {
                    player.sendMessage(NClaim.inst().getLangManager().getString("command.player.not_found"));
                    return;
                }

                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.transfer_claim.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.transfer_claim.lore").stream()
                                .map(s -> s.replace("{player}", coopPlayer.getName()))
                                .collect(Collectors.toList()),
                        result -> {
                            if ("confirmed".equals(result)) {
                                player.closeInventory();
                                claim.setOwner(coopPlayer.getUniqueId());
                                player.sendMessage(NClaim.inst().getLangManager().getString("claim.transferred")
                                        .replace("{target}", coopPlayer.getName()));
                            } else if ("declined".equals(result)) {
                                new CoopPermissionsMenu(player, coopPlayer, claim, admin, currentCategory);
                            }
                        });
            }

        });
    }

    private void addKickButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BARRIER)
                        .name(getString("kick.display_name"))
                        .lore(getStringList("kick.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.kick_coop.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.kick_coop.lore").stream()
                                .map(s -> s.replace("{player}", coopPlayer.getName()))
                                .collect(Collectors.toList()),
                        result -> {
                            if ("confirmed".equals(result)) {
                                player.closeInventory();
                                NClaim.inst().getClaimCoopManager()
                                        .removeCoopPlayer(claim, player, coopPlayer.getUniqueId());
                            } else if ("declined".equals(result)) {
                                new CoopPermissionsMenu(player, coopPlayer, claim, admin, currentCategory);
                            }
                        });
            }
        });
    }
}