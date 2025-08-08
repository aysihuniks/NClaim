package nesoi.aysihuniks.nclaim.api.events;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ClaimBuyLandEvent extends ClaimBuyEvent {

    private static final HandlerList handlers = new HandlerList();

    private final @NotNull Chunk land;

    public ClaimBuyLandEvent(@NotNull Player sender, @NotNull Claim claim, @NotNull Chunk land) {
        super(sender, claim);
        this.land = land;
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
