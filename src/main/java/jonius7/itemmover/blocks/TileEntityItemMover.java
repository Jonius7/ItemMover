package jonius7.itemmover.blocks;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Inventory ---
    private ItemStack[] inventory = new ItemStack[1];

    // --- Configurable fields ---
    private int inputSlot = 0;
    private int outputSlot = 0;
    private int inputSide = 2;       // side to pull from (0=down,1=up,2=north,3=south,4=west,5=east)
    private int outputSide = 3;      // side to push to

    // --- Update each tick ---
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        // Pull from input side
        int inX = xCoord + Facing.offsetsXForSide[inputSide];
        int inY = yCoord + (inputSide == 0 ? -1 : inputSide == 1 ? 1 : 0);
        int inZ = zCoord + Facing.offsetsZForSide[inputSide];

        TileEntity inTe = worldObj.getTileEntity(inX, inY, inZ);
        if (inTe instanceof IInventory) {
            IInventory inInv = (IInventory) inTe;
            ItemStack stack = inInv.getStackInSlot(targetSlot);
            if (stack != null) {
                ItemStack taken = inInv.decrStackSize(targetSlot, 1);
                insertItem(0, taken);
            }
        }

        // Push to output side
        int outX = xCoord + Facing.offsetsXForSide[outputSide];
        int outY = yCoord + (outputSide == 0 ? -1 : outputSide == 1 ? 1 : 0);
        int outZ = zCoord + Facing.offsetsZForSide[outputSide];

        TileEntity outTe = worldObj.getTileEntity(outX, outY, outZ);
        if (outTe instanceof IInventory && inventory[0] != null) {
            ItemStack remaining = insertIntoInventory((IInventory) outTe, inventory[0]);
            inventory[0] = remaining;
            markDirty();
        }
    }

    // --- Helper: Insert into another inventory ---
    private ItemStack insertIntoInventory(IInventory inv, ItemStack stack) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (slot == null) {
                inv.setInventorySlotContents(i, stack);
                return null;
            } else if (slot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                int max = Math.min(slot.getMaxStackSize(), inv.getInventoryStackLimit());
                int newSize = slot.stackSize + stack.stackSize;
                if (newSize <= max) {
                    slot.stackSize = newSize;
                    return null;
                } else {
                    slot.stackSize = max;
                    stack.stackSize = newSize - max;
                }
            }
        }
        return stack; // leftover if it could not fit
    }
    
    private void insertItem(int slot, ItemStack stack) {
        if (stack == null) return;

        if (inventory[slot] == null) {
            inventory[slot] = stack;
        } else if (inventory[slot].isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inventory[slot], stack)) {
            int max = Math.min(inventory[slot].getMaxStackSize(), getInventoryStackLimit());
            int total = inventory[slot].stackSize + stack.stackSize;
            inventory[slot].stackSize = Math.min(total, max);
        }
        markDirty();
    }

    // --- IInventory Implementation ---
    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] != null) {
            ItemStack itemstack;
            if (inventory[slot].stackSize <= amount) {
                itemstack = inventory[slot];
                inventory[slot] = null;
                markDirty();
                return itemstack;
            } else {
                itemstack = inventory[slot].splitStack(amount);
                if (inventory[slot].stackSize == 0) inventory[slot] = null;
                markDirty();
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "container.itemMover";
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
    public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this &&
                player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}
    @Override
    public void closeInventory() {}
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (inventory[slot] != null) {
            ItemStack stack = inventory[slot];
            inventory[slot] = null;
            return stack;
        }
        return null;
    }

    // --- Config Getters/Setters ---
    public int getTargetSlot() { return targetSlot; }
    public void setTargetSlot(int slot) { targetSlot = slot; markDirty(); }

    public int getInputSide() { return inputSide; }
    public void setInputSide(int side) { inputSide = side; markDirty(); }

    public int getOutputSide() { return outputSide; }
    public void setOutputSide(int side) { outputSide = side; markDirty(); }

    // --- Save/Load NBT ---
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("TargetSlot", targetSlot);
        tag.setInteger("InputSide", inputSide);
        tag.setInteger("OutputSide", outputSide);
        if (inventory[0] != null) {
            NBTTagCompound itemTag = new NBTTagCompound();
            inventory[0].writeToNBT(itemTag);
            tag.setTag("ItemSlot0", itemTag);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        targetSlot = tag.getInteger("TargetSlot");
        inputSide = tag.getInteger("InputSide");
        outputSide = tag.getInteger("OutputSide");
        if (tag.hasKey("ItemSlot0")) {
            inventory[0] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("ItemSlot0"));
        }
    }
    
    public static String getSideName(int side) {
        switch (side) {
            case 0: return "DOWN";
            case 1: return "UP";
            case 2: return "NORTH";
            case 3: return "SOUTH";
            case 4: return "WEST";
            case 5: return "EAST";
            default: return "UNKNOWN";
        }
    }
}
