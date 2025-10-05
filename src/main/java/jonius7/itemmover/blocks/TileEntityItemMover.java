package jonius7.itemmover.blocks;

import jonius7.itemmover.gui.SimpleInventory;
import jonius7.itemmover.gui.SimpleInventoryGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Inventories ---
    //private ItemStack[] ghostInventory = new ItemStack[9];     // Ghost slots
	private ItemStack[] ghostPull; // Pull Ghost Slots
	private ItemStack[] ghostPush; // Push Ghost Slots
    private ItemStack[] internalInventory; // Real internal slots

    // --- Configurable fields ---
    //private int inputSlot = 0;
    //private int outputSlot = 0;
    private int inputSide = 2;
    private int outputSide = 3;
    
    public TileEntityItemMover() {
    	super();
        ghostPull = new ItemStack[9];
        ghostPush = new ItemStack[9];
        internalInventory = new ItemStack[18];
    }
    /*
    // --- Update each tick ---
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        // Pull from input side
        IInventory inInv = getAdjacentInventory(inputSide);
        if (inInv != null && inputSlot >= 0 && inputSlot < inInv.getSizeInventory()) {
            ItemStack inStack = inInv.getStackInSlot(inputSlot);
            if (inStack != null) {
                for (int i = 0; i < internalInventory.length; i++) {
                    if (internalInventory[i] == null) {
                        internalInventory[i] = inInv.decrStackSize(inputSlot, 1);
                        markDirty();
                        break;
                    } else if (internalInventory[i].isItemEqual(inStack) &&
                               ItemStack.areItemStackTagsEqual(internalInventory[i], inStack) &&
                               internalInventory[i].stackSize < Math.min(internalInventory[i].getMaxStackSize(), getInventoryStackLimit())) {
                        inInv.decrStackSize(inputSlot, 1);
                        internalInventory[i].stackSize += 1;
                        markDirty();
                        break;
                    }
                }
            }
        }

        // Push to output side
        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack stack = internalInventory[i];
            if (stack != null) {
                IInventory outInv = getAdjacentInventory(outputSide);
                if (outInv != null) {
                    stack = insertIntoInventory(outInv, outputSlot, stack);
                    internalInventory[i] = stack;
                    markDirty();
                }
            }
        }
    }
    

    // --- Helpers ---
    private IInventory getAdjacentInventory(int side) {
        int x = xCoord + Facing.offsetsXForSide[side];
        int y = yCoord + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int z = zCoord + Facing.offsetsZForSide[side];
        TileEntity te = worldObj.getTileEntity(x, y, z);
        return te instanceof IInventory ? (IInventory) te : null;
    }

    private ItemStack insertIntoInventory(IInventory inv, int slot, ItemStack stack) {
        if (stack == null || slot < 0 || slot >= inv.getSizeInventory()) return stack;

        ItemStack target = inv.getStackInSlot(slot);
        if (target == null) {
            inv.setInventorySlotContents(slot, stack);
            return null;
        }

        if (target.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(target, stack)) {
            int max = Math.min(target.getMaxStackSize(), inv.getInventoryStackLimit());
            int total = target.stackSize + stack.stackSize;
            if (total <= max) {
                target.stackSize = total;
                return null;
            } else {
                target.stackSize = max;
                stack.stackSize = total - max;
                return stack;
            }
        }

        return stack;
    }
	*/
    
 // Expose them as sub-inventories:
    public IInventory getInternalInventory() {
        return new SimpleInventory(internalInventory.length, internalInventory);
    }

    // Return IInventory views for GUI
    public IInventory getPullGhostInventory() {
        return new GhostWrapperInventory(ghostPull);
    }

    public IInventory getPushGhostInventory() {
        return new GhostWrapperInventory(ghostPush);
    }
    
    /** Internal class: wrap an existing ItemStack[] as a ghost IInventory **/
    private static class GhostWrapperInventory extends SimpleInventoryGhost {
        private final ItemStack[] backing;

        public GhostWrapperInventory(ItemStack[] backing) {
            super(backing.length);
            this.backing = backing;
            // Initialize from backing
            for (int i = 0; i < backing.length; i++) {
                if (backing[i] != null) {
                    super.setInventorySlotContents(i, backing[i]);
                }
            }
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (index >= 0 && index < backing.length) {
                backing[index] = stack != null ? stack.copy() : null;
            }
        }
    }    
    
    // --- IInventory methods ---
    @Override
    public int getSizeInventory() {
        return internalInventory.length; // Only real inventory
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= internalInventory.length) return null;
        return internalInventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot < 0 || slot >= internalInventory.length) return null;

        ItemStack stack = internalInventory[slot];
        if (stack != null) {
            if (stack.stackSize <= amount) {
                internalInventory[slot] = null;
                markDirty();
                sendUpdatePacket();
                return stack;
            } else {
                ItemStack split = stack.splitStack(amount);
                if (stack.stackSize == 0) internalInventory[slot] = null;
                markDirty();
                sendUpdatePacket();
                return split;
            }
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= internalInventory.length) return;
        internalInventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit())
            stack.stackSize = getInventoryStackLimit();
        markDirty();
        sendUpdatePacket();
    }

    @Override
    public String getInventoryName() { return "container.itemMover"; }

    @Override
    public boolean hasCustomInventoryName() { return false; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
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
        if (slot < 0 || slot >= internalInventory.length) return null;
        ItemStack stack = internalInventory[slot];
        internalInventory[slot] = null;
        return stack;
    }

 // --- Ghost inventory access ---
    public int getGhostSize(boolean pull) {
        return pull ? getGhostPull().length : getGhostPush().length;
    }

    public ItemStack getGhostStack(int slot, boolean pull) {
        if (pull) {
            return getGhostPull()[slot];
        } else {
            return getGhostPush()[slot];
        }
    }

    public void setGhostStack(int slot, ItemStack stack, boolean pull) {
        if (pull) {
            getGhostPull()[slot] = stack;
        } else {
            getGhostPush()[slot] = stack;
        }
        markDirty();
        sendUpdatePacket();
    }

    // Internal Inventory get/set
    public ItemStack getInternalStack(int slot) { return internalInventory[slot]; }
    public void setInternalStack(int slot, ItemStack stack) { internalInventory[slot] = stack; }
    
    // Ghost slots methods
    public ItemStack getGhostPull(int slot) {
        if (slot < 0 || slot >= ghostPull.length) return null;
        return ghostPull[slot];
    }

    public void setGhostPull(int slot, ItemStack stack) {
        if (slot < 0 || slot >= ghostPull.length) return;
        ghostPull[slot] = stack;
        markDirty();
    }

    public ItemStack getGhostPush(int slot) {
        if (slot < 0 || slot >= ghostPush.length) return null;
        return ghostPush[slot];
    }

    public void setGhostPush(int slot, ItemStack stack) {
        if (slot < 0 || slot >= ghostPush.length) return;
        ghostPush[slot] = stack;
        markDirty();
    }


    // --- TileEntity sync ---
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    private void sendUpdatePacket() {
        if (!worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    // --- NBT ---
    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        //System.out.println("WRITE sides: " + inputSide + "," + outputSide);
        compound.setInteger("InputSide", inputSide);
        compound.setInteger("OutputSide", outputSide);
        //System.out.println("WRITE sides: " + inputSide + "," + outputSide);
        // Internal inventory
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i] != null) {
                NBTTagCompound tag = new NBTTagCompound();
                internalInventory[i].writeToNBT(tag);
                compound.setTag("InternalSlot" + i, tag);
            }
        }

        // Ghost Pull
        NBTTagList pullList = new NBTTagList();
        for (int i = 0; i < ghostPull.length; i++) {
            if (ghostPull[i] != null) {
                NBTTagCompound t = new NBTTagCompound();
                t.setByte("Slot", (byte) i);
                ghostPull[i].writeToNBT(t);
                pullList.appendTag(t);
            }
        }
        compound.setTag("GhostPull", pullList);

        // Ghost Push
        NBTTagList pushList = new NBTTagList();
        for (int i = 0; i < ghostPush.length; i++) {
            if (ghostPush[i] != null) {
                NBTTagCompound t = new NBTTagCompound();
                t.setByte("Slot", (byte) i);
                ghostPush[i].writeToNBT(t);
                pushList.appendTag(t);
            }
        }
        compound.setTag("GhostPush", pushList);
    }


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        //System.out.println("READ sides: " + inputSide + "," + outputSide);
        // Ensure arrays exist
        if (ghostPull == null) ghostPull = new ItemStack[9];
        if (ghostPush == null) ghostPush = new ItemStack[9];
        if (internalInventory == null) internalInventory = new ItemStack[18];

        if (compound.hasKey("InputSide")) {
            inputSide = compound.getInteger("InputSide");
        }
        if (compound.hasKey("OutputSide")) {
            outputSide = compound.getInteger("OutputSide");
        }
        //System.out.println("READ sides: " + inputSide + "," + outputSide);
        for (int i = 0; i < internalInventory.length; i++) {
            String key = "InternalSlot" + i;
            if (compound.hasKey(key)) {
                internalInventory[i] = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(key));
            } else {
                internalInventory[i] = null;
            }
        }

        NBTTagList pullList = compound.getTagList("GhostPull", 10);
        for (int i = 0; i < pullList.tagCount(); i++) {
            NBTTagCompound t = pullList.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < ghostPull.length) {
                ghostPull[slot] = ItemStack.loadItemStackFromNBT(t);
            }
        }

        NBTTagList pushList = compound.getTagList("GhostPush", 10);
        for (int i = 0; i < pushList.tagCount(); i++) {
            NBTTagCompound t = pushList.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < ghostPush.length) {
                ghostPush[slot] = ItemStack.loadItemStackFromNBT(t);
            }
        }
    }

    // --- Config getters/setters ---
    /*
    public int getInputSlot() { return inputSlot; }
    public void setInputSlot(int slot) { inputSlot = slot; markDirty(); sendUpdatePacket(); }

    public int getOutputSlot() { return outputSlot; }
    public void setOutputSlot(int slot) { outputSlot = slot; markDirty(); sendUpdatePacket(); }
    */
    
    public int getInputSide() { return inputSide; }
    public int getOutputSide() { return outputSide; }
    
    public void setInputSide(int side) {
        inputSide = (side + 6) % 6; // wrap 0-5
        markDirty();
        sendUpdatePacket(); // optional, only if you want instant GUI update
    }
    public void setOutputSide(int side) {
        outputSide = (side + 6) % 6;
        markDirty();
        sendUpdatePacket();
    }
    public void cycleInputSide(boolean forward) {
        setInputSide(inputSide + (forward ? 1 : -1));
    }
    public void cycleOutputSide(boolean forward) {
        setOutputSide(outputSide + (forward ? 1 : -1));
    }

    public ItemStack[] getGhostPull() {
        return ghostPull;
    }

    public void setGhostPull(ItemStack[] arr) {
        if (arr == null) return;
        // Copy array safely
        int len = Math.min(arr.length, ghostPull.length);
        for (int i = 0; i < len; i++) {
            ghostPull[i] = arr[i] != null ? arr[i].copy() : null;
        }
        markDirty();
    }

    public ItemStack[] getGhostPush() {
        return ghostPush;
    }

    public void setGhostPush(ItemStack[] arr) {
        if (arr == null) return;
        int len = Math.min(arr.length, ghostPush.length);
        for (int i = 0; i < len; i++) {
            ghostPush[i] = arr[i] != null ? arr[i].copy() : null;
        }
        markDirty();
    }
}
