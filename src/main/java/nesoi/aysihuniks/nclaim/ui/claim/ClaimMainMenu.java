package nesoi.aysihuniks.nclaim.ui.claim;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminAllClaimMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.util.ItemCreator;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.message.ChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClaimMainMenu extends BaseMenu {

    public ClaimMainMenu(Player player) {
        super("claim_menu");
        setupMenu(player);
        displayTo(player);
    }

    private void setupMenu(Player player) {
        createInventory(MenuType.CHEST_3_ROWS, getString("title"));
        addBuyClaimButton();
        addManageClaimsButton();
        
        if (player.hasPermission("nclaim.admin")) {
            addAdminButton();
        }
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private void addBuyClaimButton() {
        addButton(new Button() {


            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {

                List<String> lore = getStringList("buy_claim.lore");
                lore.replaceAll(l -> l.replace("{price}", String.valueOf(NClaim.inst().getNconfig().getClaimBuyPrice())));
                
                return ItemCreator.of(getMaterial("buy_claim"))
                        .name(getString("buy_claim.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (clickType == ClickType.LEFT) {
                    handleBuyClaimClick(player);
                } else if(clickType == ClickType.RIGHT) {
                    NClaim.inst().getClaimVisualizerService().showClaimBorders(player);
                }

            }
        });
    }

    private void handleBuyClaimClick(Player player) {
        Consumer<String> onFinish = (result) -> {
            if ("confirmed".equals(result)) {
                player.closeInventory();
                NClaim.inst().getClaimService().buyNewClaim(player);
            } else if ("declined".equals(result)) {
                new ClaimMainMenu(player);
            }
        };

        new ConfirmMenu(player,
                NClaim.inst().getGuiLangManager().getString("confirm_menu",  "children.buy_new_claim.display_name"),
                NClaim.inst().getGuiLangManager().getStringList("confirm_menu", "children.buy_new_claim.lore")
                        .stream()
                        .map(s -> s.replace("{price}", String.valueOf(NClaim.inst().getNconfig().getClaimBuyPrice())))
                        .collect(Collectors.toList()),
                onFinish);
    }

    private void addManageClaimsButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(15);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("manage_claims"))
                        .name(getString("manage_claims.display_name"))
                        .lore(getStringList("manage_claims.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ClaimListMenu(player, 0);
            }
        });
    }

    private void addAdminButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("admin"))
                        .name(getString("admin.display_name"))
                        .lore(getStringList("admin.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new AdminAllClaimMenu(player, null, true, 0, new ArrayList<>());
            }
        });
    }
}