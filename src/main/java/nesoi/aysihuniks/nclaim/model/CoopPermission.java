package nesoi.aysihuniks.nclaim.model;

import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.enums.PermissionCategory;

import java.util.EnumMap;
import java.util.Map;

public class CoopPermission {
    private final Map<Permission, Boolean> permissionStates;

    public CoopPermission() {
        this.permissionStates = new EnumMap<>(Permission.class);
        for (Permission permission : Permission.values()) {
            permissionStates.put(permission, false);
        }
    }

    public boolean isEnabled(Permission permission) {
        return permissionStates.getOrDefault(permission, false);
    }

    public void setEnabled(Permission permission, boolean enabled) {
        permissionStates.put(permission, enabled);
    }

    public void toggle(Permission permission) {
        setEnabled(permission, !isEnabled(permission));
    }

    public boolean hasAllPermissionsInCategory(PermissionCategory category) {
        return category.getPermissions().stream()
                .allMatch(this::isEnabled);
    }

    public void setAllPermissionsInCategory(PermissionCategory category, boolean state) {
        category.getPermissions().forEach(permission -> 
            permissionStates.put(permission, state));
    }

    public Map<PermissionCategory, Permission[]> getPermissionsByCategory() {
        Map<PermissionCategory, Permission[]> result = new EnumMap<>(PermissionCategory.class);
        for (PermissionCategory category : PermissionCategory.values()) {
            result.put(category, category.getPermissions().toArray(new Permission[0]));
        }
        return result;
    }

    public Map<Permission, Boolean> getAllPermissions() {
        return new EnumMap<>(permissionStates);
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Permission, Boolean> entry : permissionStates.entrySet()) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(entry.getKey().name())
                   .append(":")
                   .append(entry.getValue() ? "1" : "0");
        }
        return builder.toString();
    }

    public static CoopPermission deserialize(String data) {
        CoopPermission permission = new CoopPermission();
        if (data == null || data.isEmpty()) {
            return permission;
        }

        String[] pairs = data.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                try {
                    Permission perm = Permission.valueOf(parts[0]);
                    boolean value = "1".equals(parts[1]);
                    permission.setEnabled(perm, value);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return permission;
    }

    public void reset() {
        for (Permission permission : Permission.values()) {
            permissionStates.put(permission, false);
        }
    }

    public void copyFrom(CoopPermission other) {
        this.permissionStates.clear();
        this.permissionStates.putAll(other.permissionStates);
    }
}