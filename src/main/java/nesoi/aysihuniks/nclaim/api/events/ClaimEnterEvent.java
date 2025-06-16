package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimEnterEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final @NotNull Claim claim;
    private final @NotNull Player player;

    public ClaimEnterEvent(@NotNull Claim claim, @NotNull Player player) {
        this.claim = claim;
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
