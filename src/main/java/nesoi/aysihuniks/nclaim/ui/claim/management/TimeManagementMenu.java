package nesoi.aysihuniks.nclaim.ui.claim.management;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.button.Button;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.message.ChannelType;
import org.nandayo.dapi.object.DMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class TimeManagementMenu extends BaseMenu {
    private int days;
    private int hours;
    private int minutes;
    private int timeUnit;
    private final @NotNull Claim claim;
    private boolean admin;

    public TimeManagementMenu(@NotNull Player player, int days, int hours, int minutes, int timeUnit, @NotNull Claim claim, boolean admin) {
        super("claim_time_management_menu");
        this.claim = claim;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.timeUnit = timeUnit;
        this.admin = admin;

        setupMenu();
        displayTo(player);
    }

    @Override
    public Function<Integer, @Nullable SingleSlotButton> backgroundButtonFunction() {
        return BackgroundMenu::getButton;
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_5_ROWS, getString("title"));

        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                List<String> lore = new ArrayList<>(getStringList("claim_info.lore"));
                lore.replaceAll(s -> s.replace("{time_left}", NClaim.inst().getClaimExpirationManager().getFormattedTimeLeft(claim))
                        .replace("{expires_at}", NClaim.serializeDate(claim.getExpiredAt())));
                return ItemCreator.of(NClaim.getMaterial(DMaterial.GLOW_ITEM_FRAME, DMaterial.ITEM_FRAME))
                        .name(getString("claim_info.display_name"))
                        .lore(lore)
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
                double totalPrice = calculateTotalPrice();
                double tax = totalPrice * NClaim.inst().getNconfig().getTimeExtensionTaxRate();
                double finalPrice = totalPrice + tax;

                List<String> lore = new ArrayList<>(getStringList("confirm.lore"));
                lore.replaceAll(s -> s.replace("{price}", String.format("%.2f", finalPrice)).replace("{d}", String.valueOf(days)).replace("{h}", String.valueOf(hours)).replace("{m}", String.valueOf(minutes)));

                return ItemCreator.of(Material.BLUE_ICE)
                        .name(getString("confirm.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                if (days == 0 && hours == 0 && minutes == 0) {
                    ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("error.no_time_selected"));
                    MessageType.FAIL.playSound(player);
                    return;
                }

                double totalPrice = calculateTotalPrice();
                double tax = totalPrice * NClaim.inst().getNconfig().getTimeExtensionTaxRate();
                double finalPrice = totalPrice + tax;

                if (NClaim.inst().getBalanceSystem() == Balance.PLAYERDATA) {
                    User user = User.getUser(player.getUniqueId());
                    if (user == null) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player_data_not_found"));
                        MessageType.FAIL.playSound(player);
                        return;
                    }

                    if (user.getBalance() < finalPrice) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.balance.not_enough"));
                        MessageType.FAIL.playSound(player);
                        return;
                    }

                    user.addBalance(-finalPrice);
                } else {
                    if (NClaim.inst().getEconomy().getBalance(player) < finalPrice) {
                        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.balance.not_enough"));
                        MessageType.FAIL.playSound(player);
                        return;
                    }

                    NClaim.inst().getEconomy().withdrawPlayer(player, finalPrice);
                }

                NClaim.inst().getClaimExpirationManager().extendClaimExpiration(claim, days, hours, minutes);

                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.expiration_extended")
                        .replace("{d}", String.valueOf(days))
                        .replace("{h}", String.valueOf(hours))
                        .replace("{m}", String.valueOf(minutes))
                        .replace("{price}", String.format("%.2f", finalPrice)));

                MessageType.CONFIRM.playSound(player);
                player.closeInventory();
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
                        .name(NClaim.inst().getGuiLangManager().getString("back.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.MENU_BACK.playSound(player);
                new ClaimManagementMenu(player, claim, admin);
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
                lore.replaceAll(s -> s.replace("{days_status}", timeUnit == 0 ? "&e" + getString("days_status") : "&7" + getString("days_status"))
                        .replace("{hours_status}", timeUnit == 1 ? "&e" + getString("hours_status") : "&7" + getString("hours_status"))
                        .replace("{minutes_status}", timeUnit == 2 ? "&e" + getString("minutes_status") : "&7" + getString("minutes_status")));
                return ItemCreator.of(Material.CHAIN)
                        .name(getString("select_time_unit.display_name"))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                timeUnit = (timeUnit + 1) % 3;
                MessageType.MENU_REFRESH.playSound(player);
                new TimeManagementMenu(player, days, hours, minutes, timeUnit, claim, admin);
            }
        });

        addTimeButtons();
    }

    private double calculateTotalPrice() {
        return (days * NClaim.inst().getNconfig().getTimeExtensionPricePerDay()) +
               (hours * NClaim.inst().getNconfig().getTimeExtensionPricePerHour()) +
               (minutes * NClaim.inst().getNconfig().getTimeExtensionPricePerMinute());
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
                new TimeManagementMenu(player, days, hours, minutes, timeUnit, claim, admin);
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
                new TimeManagementMenu(player, days, hours, minutes, timeUnit, claim, admin);
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
                new TimeManagementMenu(player, days, hours, minutes, timeUnit, claim, admin);
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
                new TimeManagementMenu(player, days, hours, minutes, timeUnit, claim, admin);
            }
        });
    }

    private String getTimeUnitString() {
        switch (timeUnit) {
            case 1:
                return getString("hours_status");
            case 2:
                return getString("minutes_status");
            default:
                return getString("days_status");
        }
    }

    private void adjustTime(int amount) {
        switch (timeUnit) {
            case 0:
                days = Math.max(0, days + amount);
                break;
            case 1:
                hours = Math.max(0, hours + amount);
                break;
            case 2:
                minutes = Math.max(0, minutes + amount);
                break;
        }
    }
}