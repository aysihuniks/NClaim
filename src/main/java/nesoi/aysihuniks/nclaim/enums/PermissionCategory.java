package nesoi.aysihuniks.nclaim.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum PermissionCategory {
    BLOCKS("Block Permissions", 
            Permission.BREAK_BLOCKS, Permission.BREAK_SPAWNER,
            Permission.PLACE_BLOCKS, Permission.PLACE_SPAWNER),
            
    CONTAINERS("Container Permissions",
            Permission.USE_CHEST, Permission.USE_TRAPPED_CHEST, Permission.USE_FURNACE,
            Permission.USE_BARREL, Permission.USE_SHULKER,
            Permission.USE_HOPPER, Permission.USE_DISPENSER, Permission.USE_DROPPER),
            
    REDSTONE("Redstone Permissions",
            Permission.USE_REPEATER,
            Permission.USE_COMPARATOR, Permission.USE_BUTTONS,
            Permission.USE_PRESSURE_PLATES, Permission.USE_LEVERS),
            
    DOORS("Door Permissions",
            Permission.USE_DOORS, Permission.USE_TRAPDOORS,
            Permission.USE_GATES),
            
    WORKSTATIONS("Workstation Permissions",
            Permission.USE_CRAFTING, Permission.USE_ENCHANTING,
            Permission.USE_ANVIL, Permission.USE_GRINDSTONE,
            Permission.USE_STONECUTTER, Permission.USE_LOOM,
            Permission.USE_SMITHING, Permission.USE_CARTOGRAPHY,
            Permission.USE_BREWING),
            
    INTERACTIONS("Interaction Permissions",
            Permission.USE_BELL, Permission.USE_BEACON,
            Permission.USE_JUKEBOX, Permission.USE_NOTEBLOCK,
            Permission.USE_CAMPFIRE, Permission.USE_SOUL_CAMPFIRE, Permission.USE_BED,
            Permission.INTERACT_ARMOR_STAND, Permission.INTERACT_ITEM_FRAME),
            
    LIQUIDS("Liquid Permissions",
            Permission.PLACE_WATER, Permission.PLACE_LAVA,
            Permission.TAKE_WATER, Permission.TAKE_LAVA),
            
    ENTITIES("Entity Permissions",
            Permission.INTERACT_VILLAGER, Permission.LEASH_MOBS,
            Permission.RIDE_ENTITIES),

    COOP_PERMISSIONS("Coop Permissions",
            Permission.OPEN_CLAIM_MENU, Permission.EXPAND_CLAIM,
            Permission.ADD_COOP, Permission.EXTEND_EXPIRATION,
            Permission.MANAGE_SETTINGS, Permission.MANAGE_CLAIM_BLOCK_TYPES);

    private final String displayName;
    private final Set<Permission> permissions;

    PermissionCategory(String displayName, Permission... permissions) {
        this.displayName = displayName;
        this.permissions = Arrays.stream(permissions).collect(Collectors.toSet());
    }

    public static PermissionCategory getCategoryForPermission(Permission permission) {
        return Arrays.stream(values())
                .filter(category -> category.permissions.contains(permission))
                .findFirst()
                .orElse(null);
    }
}