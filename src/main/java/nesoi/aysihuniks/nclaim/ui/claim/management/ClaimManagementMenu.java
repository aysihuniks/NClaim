package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.ui.claim.admin.AdminAllClaimMenu;
import nesoi.aysihuniks.nclaim.ui.claim.coop.CoopListMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;

public class ClaimManagementMenu extends BaseMenu {
    private final @NotNull Claim claim;
    private final Player player;
    private final boolean admin;

    public ClaimManagementMenu(Player player, @NotNull Claim claim, boolean admin) {
        super("claim_management_menu");
        this.claim = claim;
        this.player = player;
        this.admin = admin;
        setupMenu();
        displayTo(player);
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_3_ROWS, getString("title"));

        if (admin) {
            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(10);
                }

                @Override
                public @Nullable ItemStack getItem() {
                    return ItemCreator.of(getMaterialFullPath("back"))
                            .name(NClaim.inst().getGuiLangManager().getString("back.display_name"))
                            .get();
                }

                @Override
                public void onClick(@NotNull Player p, @NotNull ClickType clickType) {
                    new AdminAllClaimMenu(p, null, true, 0, new ArrayList<>());
                }
            });
        }
        
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return admin ? Sets.newHashSet(12) : Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("expand"))
                        .name(getString("expand.display_name"))
                        .lore(getStringList("expand.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!claim.isOwner(player.getUniqueId()) && !admin && !NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.EXPAND_CLAIM)) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                MessageType.MENU_FORWARD.playSound(player);
                new LandExpansionMenu(player, claim, admin);
            }
        });
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return admin ? Sets.newHashSet(13) : Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("time"))
                        .name(getString("time.display_name"))
                        .lore(getStringList("time.lore"))
                        .get();
            }
            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!claim.isOwner(player.getUniqueId()) && !admin && !NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.EXTEND_EXPIRATION)) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                MessageType.MENU_FORWARD.playSound(player);
                new TimeManagementMenu(player, 0,0,0,0, claim, admin);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return admin ? Sets.newHashSet(14) : Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("coop"))
                        .name(getString("coop.display_name"))
                        .lore(getStringList("coop.lore"))
                        .flags(ItemFlag.values())
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!claim.isOwner(player.getUniqueId()) && !admin && !NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.ADD_COOP)) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                MessageType.MENU_FORWARD.playSound(player);
                new CoopListMenu(player, claim, admin);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return admin ? Sets.newHashSet(15) : Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("setting"))
                        .name(getString("setting.display_name"))
                        .lore(getStringList("setting.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!claim.isOwner(player.getUniqueId()) && !admin && !NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.MANAGE_SETTINGS)) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                MessageType.MENU_FORWARD.playSound(player);
                new ClaimSettingsMenu(player, claim, 0, admin);
            }
        });

        if (!admin) {
            addButton(new Button() {
                @Override
                public @NotNull Set<Integer> getSlots() {
                    return Sets.newHashSet(14);
                }

                @Override
                public @Nullable ItemStack getItem() {
                    return ItemCreator.of(getMaterial("type"))
                            .name(getString("type.display_name"))
                            .lore(getStringList("type.lore"))
                            .flags(ItemFlag.values())
                            .get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    if (!claim.isOwner(player.getUniqueId()) && !NClaim.inst().getClaimCoopManager().hasPermission(player, claim, Permission.MANAGE_CLAIM_BLOCK_TYPES)) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                        return;
                    }

                    MessageType.MENU_FORWARD.playSound(player);
                    new ManageClaimBlockMenu(claim, player, 0);
                }
            });
        }

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("delete"))
                        .name(getString("delete.display_name"))
                        .lore(getStringList("delete.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (!claim.isOwner(player.getUniqueId())) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
                    return;
                }

                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.delete_claim.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.delete_claim.lore"),
                        (result) -> {
                            if ("confirmed".equals(result)) {
                                claim.remove(RemoveCause.UNCLAIM);
                                player.closeInventory();
                            } else if ("declined".equals(result)) {
                                new ClaimManagementMenu(player, claim, admin);
                            }
                        });
            }
        });
    }
}