package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.enums.RemoveCause;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimRemoveEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final @NotNull Claim claim;
    private final @NotNull RemoveCause cause;

    public ClaimRemoveEvent(@NotNull Claim claim, @NotNull RemoveCause cause) {
        this.claim = claim;
        this.cause = cause;
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
