package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class ClaimCoopRemoveEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final @NotNull Player owner;
    private final @NotNull UUID coopPlayerUUID;
    private final @NotNull Claim claim;

    public ClaimCoopRemoveEvent(@NotNull Player owner, @NotNull UUID coopPlayerUUID, @NotNull Claim claim) {
        this.owner = owner;
        this.coopPlayerUUID = coopPlayerUUID;
        this.claim = claim;
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
