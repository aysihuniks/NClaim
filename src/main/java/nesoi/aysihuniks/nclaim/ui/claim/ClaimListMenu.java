package nesoi.aysihuniks.nclaim.ui.claim;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.ui.claim.management.ClaimManagementMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.*;

public class ClaimListMenu extends BaseMenu {
    private static @NotNull ListType currentListType = ListType.ALL;
    private final @NotNull Player player;
    private final int page;

    private static final int[] claimSlots = {
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private enum ListType {
        ALL, PLAYER, COOP
    }

    public ClaimListMenu(@NotNull Player player, int page) {
        super("claim_list_menu");
        this.player = player;
        this.page = page;

        User user = User.getUser(player.getUniqueId());
        if (user.getPlayerClaims().isEmpty() && user.getCoopClaims().isEmpty()) {
            player.closeInventory();
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("claim.not_found"));
            MessageType.WARN.playSound(player);
            return;
        }

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_6_ROWS, getString("title"));
        addNavigationButton();
        addListTypeButton();
        addClaimButtons();
        
        if (hasNextPage()) {
            addNextPageButton();
        }
    }

    private void addNavigationButton() {
        addButton(new Button() {
            final String buttonPath = page == 0 ? "back" : "previous_page";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterialFullPath(buttonPath))
                        .name(NClaim.inst().getGuiLangManager().getString(buttonPath + ".display_name"))
                        .lore(NClaim.inst().getGuiLangManager().getStringList(buttonPath + ".lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                if (page == 0) {
                    new ClaimMainMenu(player);
                } else {
                    new ClaimListMenu(player, page - 1);
                }

            }
        });
    }

    private void addListTypeButton() {
        addButton(new Button() {


            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>();
                lore.add(" ");
                lore.add(formatListTypeEntry(ListType.ALL, "all"));
                lore.add(formatListTypeEntry(ListType.PLAYER, "player"));
                lore.add(formatListTypeEntry(ListType.COOP, "coop"));

                return ItemCreator.of(getMaterial("list_type"))
                        .name(getString("list_type.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                currentListType = ListType.values()[(currentListType.ordinal() + 1) % ListType.values().length];
                MessageType.MENU_REFRESH.playSound(player);
                new ClaimListMenu(player, 0);
            }
        });
    }

    private String formatListTypeEntry(ListType type, String configKey) {
        return (currentListType == type ? "&e- " : "&7- ") + getString("list_type." + configKey);
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
                        .lore(NClaim.inst().getGuiLangManager().getStringList("next_page.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new ClaimListMenu(player, page + 1);
            }
        });
    }

    private void addClaimButtons() {
        List<Claim> claimsToShow = getClaimsToShow();
        int startIndex = page * claimSlots.length;
        int endIndex = Math.min(startIndex + claimSlots.length, claimsToShow.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            Claim claim = claimsToShow.get(i);
            boolean isOwner = User.getUser(player.getUniqueId()).getPlayerClaims().contains(claim);
            addClaimButton(claim, claimSlots[slotIndex], isOwner);
        }
    }

    private List<Claim> getClaimsToShow() {
        User.loadUser(player.getUniqueId());
        User user = User.getUser(player.getUniqueId());
        List<Claim> claims = new ArrayList<>();
        
        switch (currentListType) {
            case ALL:
                claims.addAll(user.getPlayerClaims());
                claims.addAll(user.getCoopClaims());
                break;
            case PLAYER:
                claims.addAll(user.getPlayerClaims());
                break;
            case COOP:
                claims.addAll(user.getCoopClaims());
                break;
        }
        return claims;
    }

    private void addClaimButton(Claim claim, int slot, boolean isOwner) {
        String buttonPath = isOwner ? "own_claims" : "coop_claims";
        Chunk chunk = claim.getChunk();

        List<String> lore = new ArrayList<>(getStringList(buttonPath + ".lore"));
        lore.replaceAll(s -> s.replace("{world}", chunk.getWorld().getName())
                .replace("{coordinates}", NClaim.getCoordinates(chunk))
                .replace("{owner}", Bukkit.getOfflinePlayer(claim.getOwner()).getName()));

        if (NClaim.inst().getNconfig().isSellEnabled() && claim.isForSale()) {
            List<String> saleLore = getStringList("claim_list_menu.on_sale_lore");
            for (String s : saleLore) {
                lore.add(s.replace("{price}", String.valueOf(claim.getSalePrice())));
            }
        }

        ItemStack item = ItemCreator.of(isOwner ? getMaterial("own_claims") : getMaterial("coop_claims"))
                .name(getString(buttonPath + ".display_name")
                        .replace("{display_slug}", claim.getDisplayName()))
                .lore(lore)
                .get();

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(slot);
            }

            @Override
            public ItemStack getItem() {
                return item;
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (isOwner || NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.OPEN_CLAIM_MENU)) {
                    new ClaimManagementMenu(player, claim, false);
                } else{
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                }
            }
        });
    }

    private boolean hasNextPage() {
        return (page + 1) * claimSlots.length < getClaimsToShow().size();
    }
}