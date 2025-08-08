package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.CoopPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class ClaimCoopPermissionToggleEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final @NotNull UUID coopPlayerUUID;
    private final @NotNull Claim claim;
    private final @NotNull Permission permission;
    private final boolean newState;

    public ClaimCoopPermissionToggleEvent(@NotNull UUID coopPlayerUUID, @NotNull Claim claim, @NotNull Permission permission, boolean newState) {
        this.coopPlayerUUID = coopPlayerUUID;
        this.claim = claim;
        this.permission = permission;
        this.newState = newState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
