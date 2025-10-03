package jonius7.itemmover.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SimpleInventoryGhost implements IInventory {
    private final ItemStack[] stacks;
    private final int size;

    public SimpleInventoryGhost(int size) {
        this.size = size;
        this.stacks = new ItemStack[size];
    }

    @Override
    public int getSizeInventory() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return stacks[index];
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        // Only ever keep a *copy with size 1*
        if (stack != null) {
            ItemStack ghost = stack.copy();
            ghost.stackSize = 1;
            stacks[index] = ghost;
        } else {
            stacks[index] = null;
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        // Ghost slots aren’t real stacks → just clear
        ItemStack ghost = stacks[index];
        stacks[index] = null;
        return ghost;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        ItemStack ghost = stacks[index];
        stacks[index] = null;
        return ghost;
    }

    @Override
    public String getInventoryName() {
        return "container.ghostInventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1; // Always 1 for ghost
    }

    @Override
    public void markDirty() {
        // No-op
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true; // GUI still usable
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true; // Any item can be "ghosted"
    }
}
