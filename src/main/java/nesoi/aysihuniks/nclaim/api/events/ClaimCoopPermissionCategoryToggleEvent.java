package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.enums.PermissionCategory;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class ClaimCoopPermissionCategoryToggleEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final @NotNull UUID coopPlayerUUID;
    private final @NotNull Claim claim;
    private final @NotNull PermissionCategory category;
    private final boolean newState;

    public ClaimCoopPermissionCategoryToggleEvent(@NotNull UUID coopPlayerUUID, @NotNull Claim claim,
                                                  @NotNull PermissionCategory category, boolean newState) {
        this.coopPlayerUUID = coopPlayerUUID;
        this.claim = claim;
        this.category = category;
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
