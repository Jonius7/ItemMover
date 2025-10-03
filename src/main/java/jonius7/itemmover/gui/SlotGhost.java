package jonius7.itemmover.gui;

import net.minecraft.inventory.Slot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotGhost extends Slot {

    public SlotGhost(IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        // Any item can be used as a ghost
        return true;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        // Prevent actually picking up ghost items
        return false;
    }

    @Override
    public void putStack(ItemStack stack) {
        if (stack == null) {
            super.putStack(null); // Clear
        } else {
            // Copy the item, but DO NOT clamp stackSize to 1
            ItemStack ghost = stack.copy();
            super.putStack(ghost);
        }
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        // For ghost slots, just clear
        this.putStack(null);
        return null;
    }
}
