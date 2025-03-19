package nesoi.network.NClaim.model;

import java.util.HashMap;

public class CoopPermission {

    public enum Permission {
        CAN_BREAK_SPAWNER,
        CAN_PLACE_SPAWNER,
        CAN_CAST_WATER_AND_LAVA,
        CAN_INTERACT_WITH_CLAIM_BEDROCK,
        CAN_PLACE_BLOCK,
        CAN_BREAK_BLOCK,
        CAN_INTERACT_WITH_CHEST,
        CAN_INTERACT_WITH_BUTTON_DOOR_PRESSURE_PLATE,
    }

    public CoopPermission() {

    }

    private final HashMap<CoopPermission.Permission, Boolean> permissions = new HashMap<CoopPermission.Permission, Boolean>() {{
        put(Permission.CAN_BREAK_SPAWNER, false);
        put(Permission.CAN_PLACE_SPAWNER, false);
        put(Permission.CAN_CAST_WATER_AND_LAVA, false);
        put(Permission.CAN_INTERACT_WITH_CLAIM_BEDROCK, false);
        put(Permission.CAN_PLACE_BLOCK, false);
        put(Permission.CAN_BREAK_BLOCK, false);
        put(Permission.CAN_INTERACT_WITH_CHEST, false);
        put(Permission.CAN_INTERACT_WITH_BUTTON_DOOR_PRESSURE_PLATE, false);
    }};

    public boolean isEnabled(Permission permission) {
        return permissions.get(permission);
    }

    public void set(Permission permission, boolean value) {
        permissions.put(permission, value);
    }

    public void toggle(Permission permission) {
        set(permission, !isEnabled(permission));
    }

    /*
    * Spawner breaking
    * Spawner placing
    * Casting water or lava
    * Interact with claim bedrock (always false)
    * Breaking blocks
    * Placing blocks
    * Interacting chests
    * Interacting with buttons, doors, pressure plates
    * */

}
