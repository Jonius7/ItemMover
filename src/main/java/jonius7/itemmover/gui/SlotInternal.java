package jonius7.itemmover.gui;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotInternal extends Slot {
    public SlotInternal(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        // Allow anything for now, or restrict to specific items
        return true;
    }
}
