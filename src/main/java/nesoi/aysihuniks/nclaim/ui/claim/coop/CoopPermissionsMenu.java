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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.util.ItemCreator;

import java.util.*;
import java.util.function.Function;
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

    public CoopPermissionsMenu(@NotNull Player player, @Nullable OfflinePlayer coopPlayer, @NotNull Claim claim, boolean admin, @Nullable PermissionCategory category) {
        super("manage_coop_player_permission_menu");
        this.coopPlayer = coopPlayer;
        this.claim = claim;
        this.admin = admin;
        this.currentCategory = category;

        setupMenu();
        displayTo(player);
    }

    private void loadCategoryIcons() {
        for (PermissionCategory category : PermissionCategory.values()) {
            CATEGORY_ICONS.put(
                    category,
                    getMaterial("categories." + category.name().toLowerCase())
            );
        }
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private final Map<PermissionCategory, ItemStack> CATEGORY_ICONS = new EnumMap<>(PermissionCategory.class);

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title").replace("{player}", coopPlayer != null && coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown"));

        loadCategoryIcons();

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
                return ItemCreator.of(currentCategory == null ? getMaterialFullPath("back") : getMaterialFullPath("previous_page"))
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
                String name = coopPlayer != null && coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown";
                String playerName = coopPlayer != null && coopPlayer.isOnline() ?
                        "&a" + name :
                        "&7" + name + " " + getString("offline");

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
                            .name(getString("categories." + category.name().toLowerCase() + ".display_name"))
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
                            .flags(ItemFlag.values())
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

    private ItemStack getPermissionIcon(Permission permission) {
        return getMaterial("permissions." + permission.name().toLowerCase(Locale.ENGLISH));
    }

    private void addTransferButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("transfer"))
                        .name(getString("transfer.display_name"))
                        .lore(getStringList("transfer.lore").stream().map(line -> line.replace("{player}", coopPlayer != null && coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown")).collect(Collectors.toList()))
                        .flags(ItemFlag.HIDE_ATTRIBUTES)
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
                                .map(s -> s.replace("{player}", coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown"))
                                .collect(Collectors.toList()),
                        result -> {
                            if ("confirmed".equals(result)) {
                                player.closeInventory();
                                claim.setOwner(coopPlayer.getUniqueId());
                                player.sendMessage(NClaim.inst().getLangManager().getString("claim.transferred")
                                        .replace("{target}", coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown"));
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
                return ItemCreator.of(getMaterial("kick"))
                        .name(getString("kick.display_name"))
                        .lore(getStringList("kick.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.kick_coop.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.kick_coop.lore").stream()
                                .map(s -> s.replace("{player}", coopPlayer != null && coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown"))
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