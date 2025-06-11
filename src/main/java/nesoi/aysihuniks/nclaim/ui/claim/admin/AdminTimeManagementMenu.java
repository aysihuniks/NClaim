package nesoi.network.NClaim.menus.claim.admin.inside;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;
import org.nandayo.DAPI.object.DMaterial;

public class ManageTimeMenu extends Menu {

    private int days;
    private int hours;
    private int minutes;
    private int timeUnit; // 0: Days, 1: Hours, 2: Minutes
    private final @NotNull Claim claim;

    public ManageTimeMenu(Player p, int days, int hours, int minutes, int timeUnit, @NotNull Claim claim) {
        createInventory(9 * 6, "NClaim - " + Bukkit.getOfflinePlayer(claim.getOwner()).getName() + "'s Claim");
        this.claim = claim;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.timeUnit = timeUnit;
        setup();
        displayTo(p);
    }

    private void setup() {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwner());
        addButton(new Button(0) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NCoreMain.getMaterial(DMaterial.GLOW_ITEM_FRAME, DMaterial.ITEM_FRAME))
                        .name("{BROWN}" + owner.getName() + "'s Claim")
                        .lore("",
                                "{WHITE}Time left: " + claim.getFancyExpireDate(),
                                "{WHITE}Expires At: " + NCoreMain.serializeDate(claim.getExpiredAt()))
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
            }
        });

        addButton(new Button(22) {
            @Override
            public ItemStack getItem() {
                String displayTime = "{PURPLE}" + days + " {WHITE}days " + hours + " {WHITE}hours " + minutes + " {WHITE}minutes";
                return ItemCreator.of(Material.CLOCK)
                        .name(displayTime)
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
            }
        });

        addButton(new Button(49) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BLUE_ICE)
                        .name("{GREEN}Confirm")
                        .lore("", "{WHITE}Time will be adjusted to:",
                                "{PURPLE}" + days + " {WHITE}days, " + hours + " {WHITE}hours, " + minutes + " {WHITE}minutes")
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                claim.addExpirationDate(days, hours, minutes);
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.success.expiration-date-changed", days, hours, minutes));
                new ManageTimeMenu(player, 0, 0, 0, timeUnit, claim);
            }
        });

        addButton(new Button(45) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW).name("{YELLOW}Go Back").get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ManageClaimMenu(player, claim);
            }
        });

        // Time Unit Switcher
        addButton(new Button(8) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.COMPASS)
                        .name("{YELLOW}Select Time Unit")
                        .lore("",
                                "{WHITE}- " + (timeUnit == 0 ? "{YELLOW}Days" : "{GRAY}Days"),
                                "{WHITE}- " + (timeUnit == 1 ? "{YELLOW}Hours" : "{GRAY}Hours"),
                                "{WHITE}- " + (timeUnit == 2 ? "{YELLOW}Minutes" : "{GRAY}Minutes")
                        )
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                timeUnit = (timeUnit + 1) % 4;
                new ManageTimeMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        // +1 Time Adjustment Button
        addButton(new Button(24) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GREEN_WOOL).name("{GREEN}+1 " + getTimeUnitString() + " Time").get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                adjustTime(1);
                new ManageTimeMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        // +6 Time Adjustment Button
        addButton(new Button(26) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.LIME_WOOL).name("{GREEN}+6 " + getTimeUnitString() + " Time").get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                adjustTime(6);
                new ManageTimeMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        // -1 Time Adjustment Button
        addButton(new Button(20) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.RED_WOOL).name("{RED}-1 " + getTimeUnitString() + " Time").get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                adjustTime(-1);
                new ManageTimeMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });

        // -6 Time Adjustment Button
        addButton(new Button(18) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.REDSTONE).name("{RED}-6 " + getTimeUnitString() + " Time").get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                adjustTime(-6);
                new ManageTimeMenu(player, days, hours, minutes, timeUnit, claim);
            }
        });
    }

    private String getTimeUnitString() {
        switch (timeUnit) {
            case 1: return "Hours";
            case 2: return "Minutes";
            default: return "Days";
        }
    }

    private void adjustTime(int amount) {
        switch (timeUnit) {
            case 0: days += amount; break;
            case 1: hours += amount; break;
            case 2: minutes += amount; break;
        }
    }
}
