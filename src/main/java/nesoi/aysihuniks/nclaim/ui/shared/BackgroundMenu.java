package nesoi.aysihuniks.nclaim.ui.shared;

import org.nandayo.dapi.guimanager.button.SingleSlotButton;

@Deprecated
public class BackgroundMenu {

    /**
     * @deprecated in favor of {@link BaseMenu#backgroundButton(int)}.
     */
    @Deprecated
    static public SingleSlotButton getButton(int slot) {
        return BaseMenu.getBackgroundButton(slot);
    }
}
