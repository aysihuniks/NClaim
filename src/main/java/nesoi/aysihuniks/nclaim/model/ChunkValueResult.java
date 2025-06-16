package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;

@Getter
public class ChunkValueResult {

    private final long value;
    private final String error;

    public ChunkValueResult(long value, String error) {
        this.value = value;
        this.error = error;
    }
}
