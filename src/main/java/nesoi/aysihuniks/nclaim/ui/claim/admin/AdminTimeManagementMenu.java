package nesoi.aysihuniks.nclaim.ui.claim.admin;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.model.Claim;
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
import org.nandayo.dapi.object.DMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdminTimeManagementMenu extends BaseMenu {
    private int days;
    private int hours;
    private int minutes;
    private int timeUnit;
    private final @NotNull Claim claim;

    public AdminTimeManagementMenu(@NotNull Player player, int days, int hours, int minutes, int timeUnit, @NotNull Claim claim) {
        super("menu.admin.manage_time_menu");
        this.claim = claim;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.timeUnit = timeUnit;

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        String ownerName = Bukkit.getOfflinePlayer(claim.getOwner()).getName() != null
                ? Bukkit.getOfflinePlayer(claim.getOwner()).getName() : "Unknown";
        createInventory(MenuType.CHEST_5_ROWS, getString("title").replace("{owner}", ownerName));
        setBackgroundButton(BackgroundMenu::getButton);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwner());
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(12);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>(getStringList("claim_info.lore"));
                lore.replaceAll(s -> s.replace("{time_left}", NClaim.inst().getClaimExpirationManager().getFormattedTimeLeft(claim))
                        .replace("{expires_at}", NClaim.serializeDate(claim.getExpiredAt())));
                return ItemCreator.of(NClaim.getMaterial(DMaterial.GLOW_ITEM_FRAME, DMaterial.ITEM_FRAME))
                        .name(getString("claim_info.display_name")
                                .replace("{owner}", owner.getName() != null ? owner.getName() : "Unknown"))
                        .lore(lore)
                        .get();
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(14);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CLOCK)
                        .name(getString("time_display.display_name")
                                .replace("{d}", String.valueOf(days))
                                .replace("{h}", String.valueOf(hours))
                                .replace("{m}", String.valueOf(minutes)))
                        .get();
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(31);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>(getStringList("confirm.lore"));
                lore.replaceAll(s -> s.replace("{d}", String.valueOf(days))
                        .replace("{h}", String.valueOf(hours))
                        .replace("{m}", String.valueOf(minutes)));
                return ItemCreator.of(Material.BLUE_ICE)
                        .name(getString("confirm.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                NClaim.inst().getClaimExpirationManager().extendClaimExpiration(claim, days, hours, minutes);

                String messageKey = (days >= 0 && hours >= 0 && minutes >= 0)
                        ? "command.add.expiration_extended"
                        : "command.remove.expiration_subtracted";

                player.sendMessage(langManager.getString(messageKey)
                        .replace("{d}", String.valueOf(Math.abs(days)))
                        .replace("{h}", String.valueOf(Math.abs(hours)))
                        .replace("{m}", String.valueOf(Math.abs(minutes))));

                MessageType.CONFIRM.playSound(player);

                new AdminTimeManagementMenu(player, 0, 0, 0, timeUnit, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(10);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.OAK_DOOR)
                        .name(langManager.getString("menu.back.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                new AdminClaimManagementMenu(player, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(16);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>(getStringList("select_time_unit.lore"));
                lore.replaceAll(s -> s.replace("{days_status}", timeUnit == 0 ? "&eDays" : "&7Days")
                        .replace("{hours_status}", timeUnit == 1 ? "&eHours" : "&7Hours")
                        .replace("{minutes_status}", timeUnit == 2 ? "&eMinutes" : "&7Minutes"));
                return ItemCreator.of(Material.CHAIN)
                        .name(getString("select_time_unit.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                timeUnit = (timeUnit + 1) % 3;
                MessageType.MENU_REFRESH.playSound(player);
                new AdminTimeManagementMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        addTimeButtons();
    }

    private void addTimeButtons() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(28);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.LIME_CONCRETE)
                        .name(getString("add_one.display_name")
                                .replace("{unit}", getTimeUnitString()))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                adjustTime(1);
                MessageType.VALUE_INCREASE.playSound(player);
                new AdminTimeManagementMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(29);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GREEN_CONCRETE)
                        .name(getString("add_six.display_name")
                                .replace("{unit}", getTimeUnitString()))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                adjustTime(6);
                MessageType.VALUE_INCREASE.playSound(player);
                new AdminTimeManagementMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(33);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.PINK_CONCRETE)
                        .name(getString("subtract_one.display_name")
                                .replace("{unit}", getTimeUnitString()))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                adjustTime(-1);
                MessageType.VALUE_DECREASE.playSound(player);
                new AdminTimeManagementMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(34);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.RED_CONCRETE)
                        .name(getString("subtract_six.display_name")
                                .replace("{unit}", getTimeUnitString()))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                adjustTime(-6);
                MessageType.VALUE_DECREASE.playSound(player);
                new AdminTimeManagementMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });
    }

    private String getTimeUnitString() {
        switch (timeUnit) {
            case 1:
                return "Hours";
            case 2:
                return "Minutes";
            default:
                return "Days";
        }
    }

    private void adjustTime(int amount) {
        switch (timeUnit) {
            case 0:
                days += amount;
                break;
            case 1:
                hours += amount;
                break;
            case 2:
                minutes += amount;
                break;
        }
    }

}