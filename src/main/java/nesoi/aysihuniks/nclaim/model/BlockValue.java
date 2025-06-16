package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class BlockValue {
    private Material material;
    private int value;

    public BlockValue(Material material, int value) {
        this.material = material;
        this.value = value;
    }
}
