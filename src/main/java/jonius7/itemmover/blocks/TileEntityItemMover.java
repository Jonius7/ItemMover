package jonius7.itemmover.blocks;

import java.util.HashMap;
import java.util.Map;

import jonius7.itemmover.gui.ContainerItemMover;
import jonius7.itemmover.gui.SimpleInventory;
import jonius7.itemmover.gui.SimpleInventoryGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Inventories ---
    //private ItemStack[] ghostInventory = new ItemStack[9];     // Ghost slots
	private ItemStack[] ghostPull; // Pull Ghost Slots
	private ItemStack[] ghostPush; // Push Ghost Slots
    private ItemStack[] internalInventory; // Real internal slots
    private int[] ghostPullSlotNumbers;
    private int[] ghostPushSlotNumbers;

    // --- Configurable fields ---
    //private int inputSlot = 0;
    //private int outputSlot = 0;
    private int inputSide = 2;
    private int outputSide = 3;
    private final int NUMBER_OF_GHOST_SLOTS = 12;
    private int[] pullSlotMapping;
    private int[] pushSlotMapping;
    
    public TileEntityItemMover() {
    	super();
        ghostPull = new ItemStack[NUMBER_OF_GHOST_SLOTS];
        ghostPush = new ItemStack[NUMBER_OF_GHOST_SLOTS];
        internalInventory = new ItemStack[18];
        ghostPullSlotNumbers = new int[NUMBER_OF_GHOST_SLOTS];
        ghostPushSlotNumbers = new int[NUMBER_OF_GHOST_SLOTS];
        // For pull and push ghost slots
        pullSlotMapping = new int[NUMBER_OF_GHOST_SLOTS];
        pushSlotMapping = new int[NUMBER_OF_GHOST_SLOTS];
        // Populate default values
        for (int i = 0; i < pullSlotMapping.length; i++) pullSlotMapping[i] = i;
        for (int i = 0; i < pushSlotMapping.length; i++) pushSlotMapping[i] = i;
        
    }
    
    @Override
    public void updateEntity() {
    	if (!worldObj.isRemote) {
            tryPullItems();
            tryPushItems();
        }
    }
    
    // Pulling Items Methods
    public void tryPullItems() {
    	// --- Get the adjacent inventory on the input side ---
        ForgeDirection dir = ForgeDirection.getOrientation(inputSide);
        TileEntity adjacent = worldObj.getTileEntity(
                xCoord + dir.offsetX,
                yCoord + dir.offsetY,
                zCoord + dir.offsetZ
        );
        if (!(adjacent instanceof IInventory)) return;
        IInventory source = (IInventory) adjacent;

        // --- Step 1: Combine duplicate ghost slots ---
        Map<String, Integer> desiredMap = new HashMap<>();
        Map<String, ItemStack> representative = new HashMap<>();
        for (ItemStack filter : ghostPull) {
            if (filter == null) continue;
            String key = getItemKey(filter);
            int count = desiredMap.getOrDefault(key, 0);
            desiredMap.put(key, count + filter.stackSize);
            representative.put(key, filter);
        }

        // --- Step 2: For each unique desired item ---
        for (Map.Entry<String, Integer> entry : desiredMap.entrySet()) {
            ItemStack filter = representative.get(entry.getKey());
            int desired = entry.getValue();

            // --- Count existing items in internal inventory ---
            int current = countMatchingInInternal(filter);

            // --- Include held stack if a player is viewing this block ---
            ItemStack held = getHeldStackIfViewerMatches(filter);
            if (held != null) current += held.stackSize;

            // --- Already satisfied? Skip ---
            if (current >= desired) continue;
            int needed = desired - current;

            // --- Step 3: Pull from source inventory ---
            for (int srcSlot = 0; srcSlot < source.getSizeInventory(); srcSlot++) {
                ItemStack srcStack = source.getStackInSlot(srcSlot);
                if (srcStack == null) continue;

                if (srcStack.isItemEqual(filter) &&
                    ItemStack.areItemStackTagsEqual(srcStack, filter)) {

                    int toMove = Math.min(srcStack.stackSize, needed);
                    ItemStack extracted = srcStack.splitStack(toMove);

                    // --- Insert into your internal inventory ---
                    insertIntoInternal(extracted);

                    // --- Update source inventory ---
                    if (srcStack.stackSize <= 0) source.setInventorySlotContents(srcSlot, null);
                    else source.setInventorySlotContents(srcSlot, srcStack);

                    source.markDirty();
                    needed -= toMove;

                    if (needed <= 0) break;
                }
            }
        }

        markDirty(); // mark TileEntity dirty for saving & updates
    }
    
    /**
     * Attempts to push items from internal inventory to the output block
     * according to ghost slot configuration and slot mappings.
     * Call this from updateEntity() on the server side.
     */
    public void tryPushItems() {
        // --- Find adjacent inventory on the output side ---
        ForgeDirection dir = ForgeDirection.getOrientation(outputSide);
        TileEntity adjacent = worldObj.getTileEntity(
                xCoord + dir.offsetX,
                yCoord + dir.offsetY,
                zCoord + dir.offsetZ
        );

        if (!(adjacent instanceof IInventory)) return;
        IInventory target = (IInventory) adjacent;

        // --- Loop through ghostPush slots ---
        for (int ghostIndex = 0; ghostIndex < ghostPush.length; ghostIndex++) {
            ItemStack filter = ghostPush[ghostIndex];
            if (filter == null) continue;

            int mappedSlot = getPushSlotMapping(ghostIndex); // slot number in target inventory
            if (mappedSlot < 0 || mappedSlot >= target.getSizeInventory()) continue;

            ItemStack existing = target.getStackInSlot(mappedSlot);

            if (existing == null) {
                // Slot empty → pull up to ghost stack size from internal
                ItemStack extracted = extractFromInternal(filter, filter.stackSize);
                if (extracted != null) target.setInventorySlotContents(mappedSlot, extracted);
            } else if (existing.isItemEqual(filter) && ItemStack.areItemStackTagsEqual(existing, filter)) {
                // Slot has correct item → top up to ghost stack size
                int needed = filter.stackSize - existing.stackSize;
                if (needed > 0) {
                    ItemStack extracted = extractFromInternal(filter, needed);
                    if (extracted != null) {
                        existing.stackSize += extracted.stackSize;
                        target.setInventorySlotContents(mappedSlot, existing);
                    }
                }
                // else already has enough → do nothing
            } else {
                // Slot has wrong item → skip
            }
        }

        target.markDirty();
        markDirty();
    }

    /**
     * Extracts up to 'amount' of items matching 'filter' from the internal inventory.
     */
    public ItemStack extractFromInternal(ItemStack filter, int amount) {
        if (filter == null || amount <= 0) return null;

        ItemStack result = null;
        int remaining = amount;

        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack stack = internalInventory[i];
            if (stack == null) continue;

            if (stack.isItemEqual(filter) && ItemStack.areItemStackTagsEqual(stack, filter)) {
                int toTake = Math.min(stack.stackSize, remaining);

                if (result == null) {
                    result = stack.copy();
                    result.stackSize = toTake;
                } else {
                    result.stackSize += toTake;
                }

                stack.stackSize -= toTake;
                if (stack.stackSize <= 0) internalInventory[i] = null;

                remaining -= toTake;
                if (remaining <= 0) break;
            }
        }

        if (result != null) markDirty();
        return result;
    }




    // Helper to get a unique key for an ItemStack
    private String getItemKey(ItemStack stack) {
        if (stack == null) return "null";
        int id = Item.getIdFromItem(stack.getItem());
        return id + ":" + stack.getItemDamage();
    }
    
    // Helper to count matching items in internal inventory
    private int countMatchingInInternal(ItemStack filter) {
        int count = 0;
        for (ItemStack slotStack : internalInventory) {
            if (slotStack != null &&
                slotStack.isItemEqual(filter) &&
                ItemStack.areItemStackTagsEqual(slotStack, filter)) {
                count += slotStack.stackSize;
            }
        }
        return count;
    }


    // Helper to insert items into internal inventory
    private void insertIntoInternal(ItemStack toInsert) {
        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack slotStack = internalInventory[i];
            if (slotStack == null) {
                internalInventory[i] = toInsert.copy();
                return;
            } else if (slotStack.isItemEqual(toInsert) &&
                       ItemStack.areItemStackTagsEqual(slotStack, toInsert) &&
                       slotStack.stackSize < slotStack.getMaxStackSize()) {
                int space = slotStack.getMaxStackSize() - slotStack.stackSize;
                int added = Math.min(space, toInsert.stackSize);
                slotStack.stackSize += added;
                toInsert.stackSize -= added;
                if (toInsert.stackSize <= 0) return;
            }
        }
    }
    
    // Account for a player holding a matching stack
    private ItemStack getHeldStackIfViewerMatches(ItemStack filter) {
        if (worldObj.isRemote) return null; // only server-side
        for (Object obj : worldObj.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player.openContainer instanceof ContainerItemMover) {
                TileEntityItemMover tile = ((ContainerItemMover) player.openContainer).getTile();
                if (tile == this) {
                    ItemStack held = player.inventory.getItemStack();
                    if (held != null &&
                        held.isItemEqual(filter) &&
                        ItemStack.areItemStackTagsEqual(held, filter)) {
                        return held;
                    }
                }
            }
        }
        return null;
    }
    
    
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
        compound.setIntArray("GhostPullSlotNumbers", ghostPullSlotNumbers);
        compound.setIntArray("GhostPullSlotNumbers", ghostPushSlotNumbers);
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
        
        NBTTagList pullMappingList = new NBTTagList();
        for (int i = 0; i < pullSlotMapping.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("GhostIndex", i);
            tag.setInteger("SlotNumber", pullSlotMapping[i]);
            pullMappingList.appendTag(tag);
        }
        compound.setTag("PullSlotMapping", pullMappingList);

        NBTTagList pushMappingList = new NBTTagList();
        for (int i = 0; i < pushSlotMapping.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("GhostIndex", i);
            tag.setInteger("SlotNumber", pushSlotMapping[i]);
            pushMappingList.appendTag(tag);
        }
        compound.setTag("PushSlotMapping", pushMappingList);
    }


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        //System.out.println("READ sides: " + inputSide + "," + outputSide);
        // Ensure arrays exist
        if (ghostPull == null) ghostPull = new ItemStack[12];
        if (ghostPush == null) ghostPush = new ItemStack[12];
        if (internalInventory == null) internalInventory = new ItemStack[18];

        if (compound.hasKey("InputSide")) {
            inputSide = compound.getInteger("InputSide");
        }
        if (compound.hasKey("OutputSide")) {
            outputSide = compound.getInteger("OutputSide");
        }
        if (compound.hasKey("GhostPullSlotNumbers")) ghostPullSlotNumbers = compound.getIntArray("GhostPullSlotNumbers");
        if (compound.hasKey("GhostPushSlotNumbers")) ghostPushSlotNumbers = compound.getIntArray("GhostPushSlotNumbers");
        
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
        
        NBTTagList pullMappingList = compound.getTagList("PullSlotMapping", 10);
        for (int i = 0; i < pullMappingList.tagCount(); i++) {
            NBTTagCompound tag = pullMappingList.getCompoundTagAt(i);
            int ghostIndex = tag.getInteger("GhostIndex");
            pullSlotMapping[ghostIndex] = tag.getInteger("SlotNumber");
        }

        NBTTagList pushMappingList = compound.getTagList("PushSlotMapping", 10);
        for (int i = 0; i < pushMappingList.tagCount(); i++) {
            NBTTagCompound tag = pushMappingList.getCompoundTagAt(i);
            int ghostIndex = tag.getInteger("GhostIndex");
            pushSlotMapping[ghostIndex] = tag.getInteger("SlotNumber");
        }
    }

    // --- Config getters/setters ---
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
    
    public int getPullSlotMapping(int ghostIndex) {
        return pullSlotMapping[ghostIndex];
    }

    public void setPullSlotMapping(int ghostIndex, int slotNumber) {
        pullSlotMapping[ghostIndex] = slotNumber;
        markDirty();
    }

    public int getPushSlotMapping(int ghostIndex) {
        return pushSlotMapping[ghostIndex];
    }

    public void setPushSlotMapping(int ghostIndex, int slotNumber) {
        pushSlotMapping[ghostIndex] = slotNumber;
        markDirty();
    }
    
    public int getInternalInventoryLength () {
    	return internalInventory.length;
    }
    
    public IInventory getInputInventory() {
        ForgeDirection dir = ForgeDirection.getOrientation(inputSide);
        int x = xCoord + dir.offsetX;
        int y = yCoord + dir.offsetY;
        int z = zCoord + dir.offsetZ;

        TileEntity te = worldObj.getTileEntity(x, y, z);
        if (te instanceof IInventory) return (IInventory) te;
        return null;
    }

    public IInventory getOutputInventory() {
        ForgeDirection dir = ForgeDirection.getOrientation(outputSide);
        int x = xCoord + dir.offsetX;
        int y = yCoord + dir.offsetY;
        int z = zCoord + dir.offsetZ;

        TileEntity te = worldObj.getTileEntity(x, y, z);
        if (te instanceof IInventory) return (IInventory) te;
        return null;
    }
}
