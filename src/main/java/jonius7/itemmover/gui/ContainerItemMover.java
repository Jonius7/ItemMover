package jonius7.itemmover.gui;

import org.lwjgl.input.Keyboard;

import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for the ItemMover block.
 * Has 9 ghost slots for filtering, 1 real internal slot, and player inventory.
 */
public class ContainerItemMover extends Container {

    private final TileEntityItemMover tile;
    
    private SimpleInventoryGhost ghostPullInv;
    private SimpleInventoryGhost ghostPushInv;
    
    public SlotGhost[] ghostPullSlots;
    public SlotGhost[] ghostPushSlots;

    public ContainerItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        this.tile = tile;
        
        //System.out.println("PULL LENGTH: " + tile.getGhostPull().length);
        //System.out.println("PUSH LENGTH: " + tile.getGhostPush().length);
        ghostPullInv = new SimpleInventoryGhost(tile.getGhostPull().length);
        ghostPushInv = new SimpleInventoryGhost(tile.getGhostPush().length);
        
        ItemStack[] ghostPullArray = tile.getGhostPull();
        if (ghostPullArray != null) {
            for (int i = 0; i < ghostPullInv.getSizeInventory(); i++) {
                ItemStack stack = i < ghostPullArray.length ? ghostPullArray[i] : null;
                if (stack != null) ghostPullInv.setInventorySlotContents(i, stack);
            }
        }

        ItemStack[] ghostPushArray = tile.getGhostPush();
        if (ghostPushArray != null) {
            for (int i = 0; i < ghostPushInv.getSizeInventory(); i++) {
                ItemStack stack = i < ghostPushArray.length ? ghostPushArray[i] : null;
                if (stack != null) ghostPushInv.setInventorySlotContents(i, stack);
            }
        }
        
        int pullSize = tile.getGhostPull().length;
        int pushSize = tile.getGhostPush().length;

        ghostPullSlots = new SlotGhost[pullSize];
        ghostPushSlots = new SlotGhost[pushSize];

        // --- Pull Ghost slots (3x4 grid) ---
        int pullStartX = 8;
        int pullStartY = 45;
        int pullSpacingX = 32;
        int pullSpacingY = 20;
        
        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = pullStartX + col * pullSpacingX;
            int y = pullStartY + row * pullSpacingY;
            
            SlotGhostPull slot = new SlotGhostPull(ghostPullInv, i, x, y, tile);
            this.addSlotToContainer(slot);
            //ghostPullSlots[i] = slot;
        }
        
        // --- Push Ghost slots (3x4 grid) ---
        int pushStartX = 135;
        int pushStartY = 45;
        int pushSpacingX = 32;
        int pushSpacingY = 20;
        
        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = pushStartX + col * pushSpacingX;
            int y = pushStartY + row * pushSpacingY;

            SlotGhostPush slot = new SlotGhostPush(ghostPushInv, i, x, y, tile);
            this.addSlotToContainer(slot);
            //ghostPushSlots[i] = slot;
        }

     // --- Real internal slots (18 slots above player inventory) ---
        int internalStartX = 48;
        int internalStartY = 130; // adjust as needed
        int internalCols = 9;
        int internalRows = 2;
        int internalSpacing = 18;

        for (int row = 0; row < internalRows; row++) {
            for (int col = 0; col < internalCols; col++) {
                int invIndex = row * internalCols + col; // 0..17
                int x = internalStartX + col * internalSpacing;
                int y = internalStartY + row * internalSpacing;
                this.addSlotToContainer(new SlotInternal(tile.getInternalInventory(), invIndex, x, y));
            }
        }

        // --- Player Inventory (3 rows x 9 columns) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + 40 + col * 18;
                int y = 84 + 90 + row * 18;
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // --- Player Hotbar (1 row x 9 columns) ---
        for (int col = 0; col < 9; col++) {
            int x = 8 + 40 + col * 18;
            int y = 142 + 90;
            this.addSlotToContainer(new Slot(playerInv, col, x, y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return getTile().isUseableByPlayer(player);
    }

    // --- Shift-click handling ---
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);
        
        if (slot instanceof SlotGhost) return null; //behaviour for ghost slots already in slotClick
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            int ghostPullStart = 0;
            int ghostPullEnd = 12;
            int ghostPushStart = 12;
            int ghostPushEnd = 24;
            int internalStart = 24;
            int internalEnd = 42;
            int playerStart = 42;
            int playerEnd = this.inventorySlots.size();

            // --- Internal slots -> player inventory ---
            if (index >= internalStart && index < internalEnd) {
                if (!mergeItemStack(stackInSlot, playerStart, playerEnd, true)) return null;
            }
            // --- Player inventory -> internal slots ---
            else if (index >= playerStart) {
                if (!mergeItemStack(stackInSlot, internalStart, internalEnd, false)) return null;
            }
            // --- Ghost slots are not shift-clickable ---
            else if (index >= ghostPullStart && index < ghostPushEnd) {
                return null;
            }

            if (stackInSlot.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }

        return itemstack;
    }
    
    /*
    @Override
    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        boolean merged = false;

        int i = reverse ? endIndex - 1 : startIndex;
        int step = reverse ? -1 : 1;

        while ((reverse ? i >= startIndex : i < endIndex) && stack != null && stack.stackSize > 0) {
            Slot slot = (Slot) this.inventorySlots.get(i);

            // --- Handle ghost slots specially ---
            if (slot instanceof SlotGhost) {
                ItemStack current = slot.getStack();

                if (current == null) {
                    // Put a copy of the held stack
                    slot.putStack(stack.copy());
                    merged = true;
                    break; // ghost slots only take one operation per shift-click
                } else if (current.isItemEqual(stack) &&
                           ItemStack.areItemStackTagsEqual(current, stack)) {
                    // Increment size without consuming the held stack
                    current.stackSize = Math.min(current.getMaxStackSize(), current.stackSize + stack.stackSize);
                    slot.putStack(current);
                    merged = true;
                    break;
                }
            } else {
                // Normal slot behavior for internal/inventory slots
                merged |= super.mergeItemStack(stack, i, i + 1, false);
            }

            i += step;
        }

        return merged;
    }
	*/
    
    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
        	
            Slot slot = (Slot) this.inventorySlots.get(slotId);

            if (slot instanceof SlotGhost) {
            	ItemStack current = slot.getStack();
                ItemStack held = player.inventory.getItemStack();
                boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                
                // Shift-click
                if (isShiftDown) {
                	//System.out.println("SELECTING SLOT ID + SHIFT: " + slotId);
                    if (held != null) {
                        slot.putStack(held.copy()); // full stack copy
                    } else {
                        slot.putStack(null);
                    }
                    tile.markDirty();
                    return held; // don't consume
                }
                /*
                // Left click
                if (mouseButton == 0) {
                    if (held != null) {
                        if (current != null && current.isItemEqual(held) &&
                            ItemStack.areItemStackTagsEqual(current, held)) {
                            current.stackSize = Math.min(current.getMaxStackSize(), current.stackSize + 1);
                            slot.putStack(current);
                        } else {
                            ItemStack copy = held.copy();
                            copy.stackSize = 1;
                            slot.putStack(copy);
                        }
                    } else {
                        slot.putStack(null);
                    }
                }

                // Right click
                if (mouseButton == 1) {
                    if (held != null) {
                        if (current != null && current.isItemEqual(held) &&
                            ItemStack.areItemStackTagsEqual(current, held)) {
                            current.stackSize--;
                            if (current.stackSize <= 0) slot.putStack(null);
                            else slot.putStack(current);
                        } else {
                            ItemStack copy = held.copy();
                            copy.stackSize = 1;
                            slot.putStack(copy);
                        }
                    } else {
                        slot.putStack(null);
                    }
                }
				*/
                return held; // always return held for ghost slots
            }
        }

        // Regular slots
        return super.slotClick(slotId, mouseButton, modifier, player);
    }

	/**
	 * @return the tile
	 */
	public TileEntityItemMover getTile() {
		return tile;
	}

}
