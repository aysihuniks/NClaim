package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import org.bukkit.Chunk;
import org.jetbrains.annotations.Nullable;

@Getter
public class ChunkAndClaim {

    private final Chunk chunk;
    private final Claim claim;
    private final String error;

    public ChunkAndClaim(@Nullable Chunk chunk, @Nullable Claim claim, @Nullable String error) {
        this.chunk = chunk;
        this.claim = claim;
        this.error = error;
    }

}
