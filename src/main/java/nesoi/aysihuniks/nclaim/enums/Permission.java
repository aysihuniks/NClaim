package nesoi.aysihuniks.nclaim.enums;

public enum Permission {
    BREAK_BLOCKS("Break normal blocks"),
    BREAK_SPAWNER("Break spawners"),
    PLACE_BLOCKS("Place normal blocks"),
    PLACE_SPAWNER("Place spawners"),

    USE_CHEST("Use chests"),
    USE_TRAPPED_CHEST("Use trapped chests"),
    USE_FURNACE("Use furnaces"),
    USE_BARREL("Use barrels"),
    USE_SHULKER("Use shulker boxes"),
    USE_HOPPER("Use hoppers"),
    USE_DISPENSER("Use dispensers"),
    USE_DROPPER("Use droppers"),

    USE_REDSTONE("Use redstone"),
    USE_REPEATER("Use repeaters"),
    USE_COMPARATOR("Use comparators"),
    USE_BUTTONS("Use buttons"),
    USE_PRESSURE_PLATES("Use pressure plates"),
    USE_LEVERS("Use levers"),

    USE_DOORS("Use doors"),
    USE_TRAPDOORS("Use trapdoors"),
    USE_GATES("Use fence gates"),

    USE_CRAFTING("Use crafting tables"),
    USE_ENCHANTING("Use enchanting tables"),
    USE_ANVIL("Use anvils"),
    USE_GRINDSTONE("Use grindstones"),
    USE_STONECUTTER("Use stonecutters"),
    USE_LOOM("Use looms"),
    USE_SMITHING("Use smithing tables"),
    USE_CARTOGRAPHY("Use cartography tables"),
    USE_BREWING("Use brewing stands"),

    USE_BELL("Use bells"),
    USE_BEACON("Use beacons"),
    USE_JUKEBOX("Use jukeboxes"),
    USE_NOTEBLOCK("Use note blocks"),
    USE_CAMPFIRE("Use campfires"),
    USE_SOUL_CAMPFIRE("Use soul campfires"),
    USE_BED("Use beds"),
    INTERACT_ARMOR_STAND("Interact with armor stands"),
    INTERACT_ITEM_FRAME("Interact with item frames"),
    USE_ENDER_PEARL("Use ender pearls"),

    PLACE_WATER("Place water"),
    PLACE_LAVA("Place lava"),
    TAKE_WATER("Take water"),
    TAKE_LAVA("Take lava"),

    INTERACT_VILLAGER("Interact with villagers"),
    LEASH_MOBS("Leash mobs"),
    RIDE_ENTITIES("Ride entities"),

    OPEN_CLAIM_MENU("Interact with claim bedrock"),
    EXTEND_EXPIRATION("Extend claim expiration"),
    EXPAND_CLAIM("Expand claim chunks"),
    ADD_COOP("Add co-op to claim"),
    MANAGE_CLAIM_BLOCK_TYPES("Manage claim block types"),
    MOVE_CLAIM_BLOCK("Move the claim block"),
    MANAGE_SETTINGS("Manage claim settings");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public PermissionCategory getCategory() {
        return PermissionCategory.getCategoryForPermission(this);
    }
}