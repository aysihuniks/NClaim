package nesoi.network.NClaim.menus.claim.coop;

import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.CoopPermission;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.*;
import java.util.function.Consumer;

import static nesoi.network.NClaim.utils.HeadManager.getPlayerHead;
import static org.nandayo.DAPI.HexUtil.parse;

public class PermissionMenu extends Menu {

    private final @NotNull Claim claim;
    private final @NotNull OfflinePlayer targetPlayer;
    private final boolean admin;

    public PermissionMenu(@NotNull Player player, @NotNull OfflinePlayer targetPlayer, @NotNull Claim claim, boolean admin) {
        createInventory(9 * 6, "NClaim - Edit Co-op");
        this.claim = claim;
        this.targetPlayer = targetPlayer;
        this.admin = admin;

        setup();
        displayTo(player);
    }

    public void setup() {
        List<CoopPermission.Permission> permissions = Arrays.asList(
                CoopPermission.Permission.CAN_BREAK_SPAWNER,
                CoopPermission.Permission.CAN_PLACE_SPAWNER,
                CoopPermission.Permission.CAN_CAST_WATER_AND_LAVA,
                CoopPermission.Permission.CAN_INTERACT_WITH_CLAIM_BEDROCK, // ALWAYS FALSE
                CoopPermission.Permission.CAN_BREAK_BLOCK,
                CoopPermission.Permission.CAN_PLACE_BLOCK,
                CoopPermission.Permission.CAN_INTERACT_WITH_CHEST,
                CoopPermission.Permission.CAN_INTERACT_WITH_BUTTON_DOOR_PRESSURE_PLATE
        );

        List<String> permissionNames = Arrays.asList(
                "Spawner Breaking",
                "Spawner Placing",
                "Casting Water and Lavas",
                "Interact With Claim Bedrock",
                "Breaking Blocks",
                "Placing Blocks",
                "Interacting with Chests",
                "Interacting with Buttons, Doors, Pressure Plates"
        );

        List<List<String>> permissionLores = Arrays.asList(
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}break {GRAY}the {WHITE}spawners{GRAY}?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}place {GRAY}the {WHITE}spawners{GRAY}?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}cast {GRAY}water or lava?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}interact {GRAY}with claim bedrock?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}break {GRAY}blocks?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}place {GRAY}blocks?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}interact with {GRAY}chests?", "&f"),
                Arrays.asList("&f", "{GRAY}Can the player {WHITE}interact with", "{GRAY}buttons, doors, pressure plates?" , "&f")
        );

        List<Integer> paperSlots = Arrays.asList(11, 20, 29, 38, 14, 23, 32, 41);
        List<Integer> dyeSlots = Arrays.asList(12, 21, 30, 39, 15, 24, 33, 42);

        addButton(new Button(45) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{ORANGE}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ManageMenu(p, claim, admin);
            }
        });

        addButton(new Button(0) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getPlayerHead(targetPlayer))
                        .name("{YELLOW}" + targetPlayer.getName())
                        .lore("", "{WHITE}You are now {GRAY}setting {WHITE}this", "{WHITE}player's {GRAY}permissions{WHITE}.")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {

            }
        });

        addButton(new Button(53) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BARRIER)
                        .name("{BROWN}Kick player from claim")
                        .lore("",
                                "{WHITE}After doing this, the player will be",
                                "{GRAY}kicked {WHITE}from the claim and you",
                                "{WHITE}will {GRAY}not be able to add {WHITE}the player",
                                "{WHITE}back to the claim for {GRAY}15 minutes{WHITE}.")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                Consumer<String> onFinish = (result) -> {
                    if ("confirmed".equals(result)) {
                        claim.kickCoop(targetPlayer.getUniqueId());
                        p.closeInventory();
                    } else if ("declined".equals(result)) {
                        new PermissionMenu(p, targetPlayer, claim, admin);
                    }
                };

                new ConfirmMenu(p, "Kick from Claim", Arrays.asList("", "{WHITE}After {GRAY}confirming {WHITE}this action,", "{WHITE}the {GRAY}selected {WHITE}person will be", "{GRAY}kicked {WHITE}from {GRAY}Claim{WHITE}."), onFinish);
            }
        });

        for (int i = 0; i < permissions.size(); i++) {
            CoopPermission.Permission permEnum = permissions.get(i);
            String permName = permissionNames.get(i);
            List<String> permLore = new ArrayList<>(permissionLores.get(i));

            boolean permissionStatus = claim.getCoopPermissionState(targetPlayer.getUniqueId(), permEnum);
            String permissionColor = permissionStatus ? "{GREEN}Active" : "{RED}Inactive";
            Material dyeMaterial = permissionStatus ? Material.LIME_DYE : Material.GRAY_DYE;

            permLore.add("{GRAY}Status: " + parse(permissionColor));

            addButton(new Button(paperSlots.get(i)) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.PAPER)
                            .name("{BROWN}" + permName)
                            .lore(permLore)
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (!permEnum.equals(CoopPermission.Permission.CAN_INTERACT_WITH_CLAIM_BEDROCK)) {
                        claim.toggleCoopPermission(targetPlayer.getUniqueId(), permEnum);
                        new PermissionMenu(p, targetPlayer, claim, admin);
                    }
                }
            });

            addButton(new Button(dyeSlots.get(i)) {
                @Override
                public ItemStack getItem() {
                    Material finalDyeMaterial = permEnum.equals(CoopPermission.Permission.CAN_INTERACT_WITH_CLAIM_BEDROCK) ? Material.RED_DYE : dyeMaterial;

                    return ItemCreator.of(finalDyeMaterial)
                            .name("{BROWN}" + permName)
                            .lore(permLore)
                            .hideFlag(ItemFlag.values())
                            .get();
                }

                @Override
                public void onClick(Player p, ClickType clickType) {
                    if (!permEnum.equals(CoopPermission.Permission.CAN_INTERACT_WITH_CLAIM_BEDROCK)) {
                        claim.toggleCoopPermission(targetPlayer.getUniqueId(), permEnum);
                        new PermissionMenu(p, targetPlayer, claim, admin);
                    }
                }
            });
        }
    }
}
