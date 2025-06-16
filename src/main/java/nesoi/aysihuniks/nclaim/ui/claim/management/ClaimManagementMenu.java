package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.ui.claim.coop.CoopListMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.ConfirmMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.utils.HeadManager;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.guimanager.Menu;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;

import java.util.Set;

public class ClaimManagementMenu extends Menu {
    private final @NotNull Claim claim;
    private final LangManager langManager;
    private final ConfigurationSection menuSection;
    private Player player;

    public ClaimManagementMenu(Player player, @NotNull Claim claim) {
        this.claim = claim;
        this.player = player;
        this.langManager = NClaim.inst().getLangManager();
        this.menuSection = langManager.getSection("menu.claim_menu");
        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_3_ROWS, langManager.getString(menuSection, "title"));
        setBackgroundButton(BackgroundMenu::getButton);
        
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name(langManager.getString(menuSection, "manage_chunks.display_name"))
                        .lore(langManager.getStringList(menuSection, "manage_chunks.lore"))
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
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CLOCK)
                        .name(langManager.getString(menuSection, "manage_expiry.display_name"))
                        .lore(langManager.getStringList(menuSection, "manage_expiry.lore"))
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
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NClaim.inst().getHeadManager().createHead(player))
                        .name(langManager.getString(menuSection, "manage_members.display_name"))
                        .lore(langManager.getStringList(menuSection, "manage_members.lore"))
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
                return Sets.newHashSet(14);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.END_CRYSTAL)
                        .name(langManager.getString(menuSection, "manage_settings.display_name"))
                        .lore(langManager.getStringList(menuSection, "manage_settings.lore"))
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
                return Sets.newHashSet(15) ;
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.TNT)
                        .name(langManager.getString(menuSection, "delete.display_name"))
                        .lore(langManager.getStringList(menuSection, "delete.lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                new ConfirmMenu(player,
                        langManager.getString("menu.confirm_menu.delete_claim.display_name"),
                        langManager.getStringList("menu.confirm_menu.delete_claim.lore"),
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