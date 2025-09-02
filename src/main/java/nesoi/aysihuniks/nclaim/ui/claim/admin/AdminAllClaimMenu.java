package nesoi.aysihuniks.nclaim.ui.claim.admin;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.integrations.AnvilManager;
import nesoi.aysihuniks.nclaim.ui.claim.ClaimMainMenu;
import nesoi.aysihuniks.nclaim.ui.claim.coop.CoopListMenu;
import nesoi.aysihuniks.nclaim.ui.claim.management.ClaimManagementMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.ClaimSetting;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.message.ChannelType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminAllClaimMenu extends BaseMenu {
    private static final int CLAIMS_PER_PAGE = 14;
    private static final int[] CLAIM_SLOTS = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final OfflinePlayer target;
    private final boolean sortByNewest;
    private final int page;
    private final List<Claim> selectedClaims;

    public AdminAllClaimMenu(Player player, OfflinePlayer target, boolean sortByNewest, int page, List<Claim> selectedClaims) {
        super("admin_menu.all_claims_menu");
        this.target = target;
        this.sortByNewest = sortByNewest;
        this.page = page;
        this.selectedClaims = new ArrayList<>(selectedClaims);

        setupMenu();
        displayTo(player);
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));

        addNavigationButton();
        addSortButton();
        addSearchButton();
        addResetButton();
        addDeleteButton();
        addClaimButtons();

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
                return ItemCreator.of(page == 0 ? getMaterialFullPath("back") : getMaterialFullPath("previous_page"))
                        .name(NClaim.inst().getGuiLangManager().getString((page == 0 ? "back" : "previous_page") + ".display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (page == 0) {
                    new ClaimMainMenu(player);
                } else {
                    new AdminAllClaimMenu(player, target, sortByNewest, page - 1, selectedClaims);
                }
            }
        });
    }

    private void addSortButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>(getStringList("sort_by_date.lore"));
                lore.replaceAll(s -> s.replace("{newest_status}", sortByNewest ? "&eNewest First" : "&7Newest First")
                        .replace("{oldest_status}", !sortByNewest ? "&eOldest First" : "&7Oldest First"));

                return ItemCreator.of(getMaterial("sort_by_date"))
                        .name(getString("sort_by_date.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new AdminAllClaimMenu(player, target, !sortByNewest, page, selectedClaims);
            }
        });
    }

    private void addSearchButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("search_player"))
                        .name(getString("search_player.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                handleSearch(player);
            }
        });
    }

    private void addResetButton() {
        if (target != null || page > 0 || !selectedClaims.isEmpty()) {
            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(14);
                }

                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(getMaterial("reset_settings"))
                            .name(getString("reset_settings.display_name"))
                            .get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    new AdminAllClaimMenu(player, null, true, 0, new ArrayList<>());
                }
            });
        }
    }

    private void addDeleteButton() {
        if (!selectedClaims.isEmpty()) {
            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(15);
                }

                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(getMaterial("delete_selected"))
                            .name(getString("delete_selected.display_name"))
                            .get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    Consumer<String> onFinish = (result) -> {
                        if ("confirmed".equals(result)) {
                            selectedClaims.forEach(claim -> claim.remove(RemoveCause.REMOVED_BY_ADMIN));
                            selectedClaims.clear();
                            new AdminAllClaimMenu(player, target, sortByNewest, page, new ArrayList<>());
                            MessageType.CONFIRM.playSound(player);
                        } else if ("declined".equals(result)) {
                            new AdminAllClaimMenu(player, target, sortByNewest, page, selectedClaims);
                        }
                    };

                    new ConfirmMenu(player,
                            NClaim.inst().getGuiLangManager().getString("confirm_menu.children.admin_delete_claim.display_name"),
                            NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.admin_delete_claim.lore"), onFinish);
                }
            });
        }
    }

    private void addNextPageButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterialFullPath("next_page"))
                        .name(NClaim.inst().getGuiLangManager().getString("next_page.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new AdminAllClaimMenu(player, target, sortByNewest, page + 1, selectedClaims);
            }
        });
    }

    private void addClaimButtons() {
        List<Claim> filteredClaims = getFilteredClaims();
        int startIndex = page * CLAIMS_PER_PAGE;
        int endIndex = Math.min(startIndex + CLAIMS_PER_PAGE, filteredClaims.size());

        for (int i = startIndex; i < endIndex; i++) {
            addClaimButton(filteredClaims.get(i), i - startIndex);
        }
    }

    private List<Claim> getFilteredClaims() {
        return Claim.getClaims().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Claim::getClaimId,
                        claim -> claim,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .filter(claim -> target == null || claim.getOwner().equals(target.getUniqueId()))
                .sorted((c1, c2) -> sortByNewest
                        ? Long.compare(c2.getCreatedAt().getTime(), c1.getCreatedAt().getTime())
                        : Long.compare(c1.getCreatedAt().getTime(), c2.getCreatedAt().getTime()))
                .collect(Collectors.toList());
    }

    private void addClaimButton(Claim claim, int index) {
        Chunk chunk = claim.getChunk();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwner());
        ClaimSetting settings = claim.getSettings();

        int enabledSettings = (int) Arrays.stream(Setting.values())
                .filter(settings::isEnabled)
                .count();
        int disabledSettings = Setting.values().length - enabledSettings;

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(CLAIM_SLOTS[index]);
            }

            @Override
            public ItemStack getItem() {
                String section = selectedClaims.contains(claim) ? "claim_items.selected" : "claim_items.unselected";
                List<String> lore = new ArrayList<>(getStringList(section + ".lore"));
                lore.replaceAll(s -> s
                        .replace("{created_at}", NClaim.serializeDate(claim.getCreatedAt()))
                        .replace("{world}", chunk.getWorld().getName())
                        .replace("{coordinates}", NClaim.getCoordinates(chunk))
                        .replace("{land_size}", String.valueOf(claim.getLands().size()))
                        .replace("{coop_count}", String.valueOf(claim.getCoopPlayers().size()))
                        .replace("{yes}", String.valueOf(enabledSettings))
                        .replace("{no}", String.valueOf(disabledSettings)));

                ItemStack baseItem;
                String materialPath = "guis." + section + ".material";

                if (NClaim.inst().getGuiLangManager().getGuiConfig().contains(materialPath)) {
                    baseItem = NClaim.inst().getGuiLangManager().getMaterial(section);
                } else {
                    baseItem = NClaim.inst().getHeadManager().createHeadFromCache(owner.getUniqueId());
                }

                return ItemCreator.of(baseItem)
                        .name(getString(section + ".display_name")
                                .replace("{owner}", owner.getName() != null ? owner.getName() : "Unknown"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                handleClaimClick(player, claim, clickType);
            }
        });
    }

    private void handleClaimClick(Player player, Claim claim, ClickType clickType) {
        if (clickType == ClickType.RIGHT) {
            if (selectedClaims.contains(claim)) {
                selectedClaims.remove(claim);
                MessageType.MENU_DESELECT.playSound(player);
            } else {
                selectedClaims.add(claim);
                MessageType.MENU_SELECT.playSound(player);
            }
            new AdminAllClaimMenu(player, target, sortByNewest, page, selectedClaims);
        } else if (clickType == ClickType.LEFT) {
            new ClaimManagementMenu(player, claim, true);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            player.closeInventory();
            teleportToClaimSafely(player, claim);
        }
    }


    private void handleSearch(Player player) {
        MessageType.SEARCH_OPEN.playSound(player);
        new AnvilManager(player, "Enter a player name",
                (text) -> {
                    if (text == null || text.isEmpty()) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_player"));
                        MessageType.FAIL.playSound(player);
                        new AdminAllClaimMenu(player, null, sortByNewest, page, selectedClaims);
                        return;
                    }

                    Set<String> matchedNames = getStrings(text);

                    if (matchedNames.isEmpty()) {
                        MessageType.FAIL.playSound(player);
                        new AdminAllClaimMenu(player, target, sortByNewest, page, selectedClaims);
                        return;
                    }

                    if (matchedNames.size() == 1) {
                        String foundName = matchedNames.iterator().next();
                        OfflinePlayer target = null;
                        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                            if (offline.getName() != null && offline.getName().equals(foundName)) {
                                target = offline;
                                break;
                            }
                        }
                        new AdminAllClaimMenu(player, target, sortByNewest, 0, selectedClaims);
                    } else {
                        new AdminAllClaimMenu(player, null, sortByNewest, 0, selectedClaims);
                    }
                });
    }

    private static @NotNull Set<String> getStrings(String text) {
        Set<String> matchedNames = new HashSet<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(text.toLowerCase())) {
                matchedNames.add(online.getName());
            }
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String name = offline.getName();
            if (name != null && name.toLowerCase().startsWith(text.toLowerCase())) {
                matchedNames.add(name);
            }
        }
        return matchedNames;
    }

    private void teleportToClaimSafely(Player player, Claim claim) {
        Location claimBlockLocation = claim.getClaimBlockLocation();
        World world = claimBlockLocation.getWorld();
        if (world == null) return;

        Location safeLoc = new Location(world,
                claimBlockLocation.getX() + 0.5,
                claimBlockLocation.getY() + 1,
                claimBlockLocation.getZ() + 0.5);

        while (safeLoc.getY() < world.getMaxHeight() && !safeLoc.getBlock().getType().isSolid()) {
            safeLoc.setY(safeLoc.getY() + 1);
        }

        if (!safeLoc.getBlock().getType().isSolid()) {
            safeLoc.setY(world.getHighestBlockYAt(safeLoc) + 1);
        }

        if (safeLoc.getY() < claimBlockLocation.getY()) {
            safeLoc.setY(claimBlockLocation.getY() + 1);
        }

        Bukkit.getScheduler().runTaskLater(NClaim.inst(), () -> {
            MessageType.TELEPORT.playSound(player);
            player.teleport(safeLoc);
        }, 1L);
    }

    private boolean hasNextPage() {
        return getFilteredClaims().size() > (page + 1) * CLAIMS_PER_PAGE;
    }
}