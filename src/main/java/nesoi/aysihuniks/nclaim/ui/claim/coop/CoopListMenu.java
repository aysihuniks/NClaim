package nesoi.aysihuniks.nclaim.ui.claim.coop;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.integrations.AnvilManager;
import nesoi.aysihuniks.nclaim.ui.claim.management.ClaimManagementMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminClaimManagementMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.CoopPermission;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.Util;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CoopListMenu extends BaseMenu {
    private final @NotNull Claim claim;
    private final boolean admin;
    private final int page;

    private static final int[] coopSlots = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public CoopListMenu(Player player, @NotNull Claim claim, Boolean admin, int page) {
        super("claim_manage_coop_menu");
        this.claim = claim;
        this.admin = admin;
        this.page = page;

        List<UUID> coopPlayers = new ArrayList<>(claim.getCoopPlayers());
        int startIndex = page * coopSlots.length;
        int endIndex = Math.min(startIndex + coopSlots.length, coopPlayers.size());
        Collection<UUID> uuidsToLoad = coopPlayers.subList(startIndex, endIndex);
        NClaim.inst().getHeadManager().preloadTexturesAsync(uuidsToLoad);

        Bukkit.getScheduler().runTaskAsynchronously(NClaim.inst(), () -> {
            setupMenu();
            Bukkit.getScheduler().runTask(NClaim.inst(), () -> displayTo(player));
        });
    }

    public CoopListMenu(Player player, @NotNull Claim claim, Boolean admin) {
        this(player, claim, admin, 0);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);

        addNavigationButton();
        addAddMemberButton();
        addMemberButtons();

        if (hasNextPage()) {
            addNextPageButton();
        }
    }

    private void addNavigationButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(page == 0 ? Material.OAK_DOOR : Material.FEATHER)
                        .name(NClaim.inst().getGuiLangManager().getString((page == 0 ? "back" : "previous_page") + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (page == 0) {
                    if (!admin) {
                        new ClaimManagementMenu(player, claim);
                    } else {
                        new AdminClaimManagementMenu(player, claim);
                    }
                } else {
                    new CoopListMenu(player, claim, admin, page - 1);
                }
            }
        });
    }

    private void addAddMemberButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.NETHER_STAR)
                        .name(getString("add_coop.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                handleAddMember(player);
            }
        });
    }

    private void handleAddMember(Player player) {
        MessageType.SEARCH_OPEN.playSound(player);
        new AnvilManager(NClaim.inst(), player, getString("search_title"),
                (text) -> {
                    if (text == null || text.isEmpty()) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_player"));
                        MessageType.FAIL.playSound(player);
                        player.closeInventory();
                        return;
                    }

                    if (text.equalsIgnoreCase(player.getName())) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player.cant_add_self"));
                        MessageType.FAIL.playSound(player);
                        player.closeInventory();
                        return;
                    }

                    Player target = Bukkit.getPlayerExact(text);
                    if (target == null) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player.not_found")
                                .replace("{target}", text));
                        MessageType.FAIL.playSound(player);
                        player.closeInventory();
                        return;
                    }

                    showAddMemberConfirmation(player, text, target);
                });
    }

    private void showAddMemberConfirmation(Player player, String targetName, Player target) {
        Consumer<String> onFinish = (result) -> {
            if ("confirmed".equals(result)) {
                player.closeInventory();
                NClaim.inst().getClaimCoopManager().addCoopPlayer(claim, player, target);
            } else if ("declined".equals(result)) {
                new CoopListMenu(player, claim, admin, page);
            }
        };

        new ConfirmMenu(player,
                NClaim.inst().getGuiLangManager().getString("confirm_menu.children.add_coop.display_name"),
                NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.add_coop.lore")
                        .stream()
                        .map(s -> s.replace("{player}", targetName))
                        .collect(Collectors.toList()),
                onFinish);
    }

    private void addNextPageButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.COMPASS)
                        .name(getString("next_page.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new CoopListMenu(player, claim, admin, page + 1);
            }
        });
    }

    private void addMemberButtons() {
        List<UUID> coopPlayers = new ArrayList<>(claim.getCoopPlayers());
        int startIndex = page * coopSlots.length;
        int endIndex = Math.min(startIndex + coopSlots.length, coopPlayers.size());

        Player owner = Bukkit.getPlayer(claim.getOwner());
        int maxCoopSlots = owner != null ? NClaim.inst().getNconfig().getMaxCoopPlayers(owner) : 3;

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            addMemberButton(coopPlayers.get(i), slotIndex);
        }

        for (int slot = 0; slot < coopSlots.length; slot++) {
            if (startIndex + slot >= coopPlayers.size()) {
                if (startIndex + slot < maxCoopSlots) {
                    addEmptySlotButton(coopSlots[slot]);
                } else {
                    addLockedSlotButton(coopSlots[slot]);
                }
            }
        }
    }

    private void addLockedSlotButton(int slot) {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(slot);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.RED_STAINED_GLASS_PANE)
                        .name(getString("locked_slot.display_name"))
                        .lore(getStringList("locked_slot.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.FAIL.playSound(player);
            }
        });
    }


    private void addMemberButton(UUID coopPlayerUUID, int slotIndex) {
        final OfflinePlayer coopPlayer = Bukkit.getOfflinePlayer(coopPlayerUUID);
        final String playerName = coopPlayer.getName() != null ? coopPlayer.getName() : "Unknown";
        final String buttonPath = "coop_info";

        CoopPermission cp = claim.getCoopPermissions().get(coopPlayerUUID);
        int enabledPerms = (int) Arrays.stream(Permission.values())
                .filter(cp::isEnabled)
                .count();
        int disabledPerms = Permission.values().length - enabledPerms;

        List<String> lore = new ArrayList<>(getStringList(buttonPath + ".lore"));
        lore.replaceAll(s -> s.replace("{date}", NClaim.serializeDate(claim.getCoopPlayerJoinDate().get(coopPlayerUUID))));

        if (admin) {
            List<String> adminLore = new ArrayList<>(getStringList(buttonPath + ".admin_lore"));
            adminLore.replaceAll(s -> s.replace("{yes}", String.valueOf(enabledPerms))
                    .replace("{no}", String.valueOf(disabledPerms)));
            lore.addAll(adminLore);
        }

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(coopSlots[slotIndex]);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NClaim.inst().getHeadManager().createHead(coopPlayer))
                        .name(getString(buttonPath + ".display_name")
                                .replace("{player}", coopPlayer.isOnline() ? "&a" + playerName : "&7" + playerName + " " + getString("offline")))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new CoopPermissionsMenu(player, coopPlayer, claim, admin, null);
            }
        });
    }

    private void addEmptySlotButton(int slot) {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(slot);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.WHITE_STAINED_GLASS_PANE)
                        .name(getString("empty_slot.display_name"))
                        .lore(getStringList("empty_slot.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                handleAddMember(player);
            }
        });
    }


    private boolean hasNextPage() {
        return (page + 1) * coopSlots.length < claim.getCoopPlayers().size();
    }
}