package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.ui.claim.coop.CoopListMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.HeadManager;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.guimanager.Menu;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.object.DMaterial;

import java.util.Set;

public class ClaimManagementMenu extends BaseMenu {
    private final @NotNull Claim claim;
    private final Player player;

    public ClaimManagementMenu(Player player, @NotNull Claim claim) {
        super("claim_management_menu");
        this.claim = claim;
        this.player = player;
        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_3_ROWS, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);
        
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name(getString("expand.display_name"))
                        .lore(getStringList("expand.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new LandExpansionMenu(player, claim, false);
            }
        });
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CLOCK)
                        .name(getString("time.display_name"))
                        .lore(getStringList("time.lore"))
                        .get();
            }
            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new TimeManagementMenu(player, 0,0,0,0, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NClaim.inst().getHeadManager().createHead(player))
                        .name(getString("coop.display_name"))
                        .lore(getStringList("coop.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new CoopListMenu(player, claim, false);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.END_CRYSTAL)
                        .name(getString("setting.display_name"))
                        .lore(getStringList("setting.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new ClaimSettingsMenu(player, claim, 0);
            }
        });
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14) ;
            }

            @Override
            public @Nullable ItemStack getItem() {
                return ItemCreator.of(NClaim.getMaterial(DMaterial.BUNDLE, DMaterial.SHULKER_BOX))
                        .name(getString("type.display_name"))
                        .lore(getStringList("type.lore"))
                        .hideFlag(ItemFlag.values())
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ManageClaimBlockMenu(claim, player, 0);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16) ;
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.TNT)
                        .name(getString("delete.display_name"))
                        .lore(getStringList("delete.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ConfirmMenu(player,
                        NClaim.inst().getGuiLangManager().getString("confirm_menu.children.delete_claim.display_name"),
                        NClaim.inst().getGuiLangManager().getStringList("confirm_menu.children.delete_claim.lore"),
                        (result) -> {
                            if ("confirmed".equals(result)) {
                                claim.remove(RemoveCause.UNCLAIM);
                                player.closeInventory();
                            } else if ("declined".equals(result)) {
                                new ClaimManagementMenu(player, claim);
                            }
                        });
            }
        });
    }
}