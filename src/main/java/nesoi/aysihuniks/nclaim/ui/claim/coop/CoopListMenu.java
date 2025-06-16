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
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

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
        super("menu.manage_coop_menu");
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
            final String buttonPath = "add_coop";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.NETHER_STAR)
                        .name(getString(buttonPath + ".display_name"))
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
        new AnvilManager(NClaim.inst(), player, "Enter a player name",
                (text) -> {
                    if (text == null || text.isEmpty()) {
                        player.sendMessage(langManager.getString("command.enter_a_player"));
                        MessageType.FAIL.playSound(player);
                        player.closeInventory();
                        return;
                    }

                    if (text.equalsIgnoreCase(player.getName())) {
                        player.sendMessage(langManager.getString("command.player.cant_add_self"));
                        MessageType.FAIL.playSound(player);
                        player.closeInventory();
                        return;
                    }

                    Player target = Bukkit.getPlayerExact(text);
                    if (target == null) {
                        player.sendMessage(langManager.getString("command.player.not_found")
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
                langManager.getString("menu.confirm_menu.add_coop.display_name"),
                langManager.getStringList("menu.confirm_menu.add_coop.lore")
                        .stream()
                        .map(s -> s.replace("{player}", targetName))
                        .collect(Collectors.toList()),
                onFinish);
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
                new CoopListMenu(player, claim, admin, page + 1);
            }
        });
    }

    private void addMemberButtons() {
        List<UUID> coopPlayers = new ArrayList<>(claim.getCoopPlayers());
        int startIndex = page * coopSlots.length;
        int endIndex = Math.min(startIndex + coopSlots.length, coopPlayers.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            addMemberButton(coopPlayers.get(i), slotIndex);
        }
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
                                .replace("{player}", coopPlayer.isOnline() ? "&a" + playerName : "&7" + playerName + " (Offline)"))
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

    private boolean hasNextPage() {
        return (page + 1) * coopSlots.length < claim.getCoopPlayers().size();
    }
}