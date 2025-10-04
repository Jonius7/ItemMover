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
        if (index < 0 || index >= size) return null;
        return stacks[index];
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= size) return;

        if (stack != null) {
            ItemStack ghost = stack.copy();
            ghost.stackSize = Math.min(stack.stackSize, getInventoryStackLimit()); // keep stack size
            stacks[index] = ghost;
        } else {
            stacks[index] = null;
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index < 0 || index >= size) return null;

        ItemStack ghost = stacks[index];
        stacks[index] = null;
        return ghost;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        if (index < 0 || index >= size) return null;

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
        return 64;
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
