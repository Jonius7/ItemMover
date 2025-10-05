package jonius7.itemmover.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jonius7.itemmover.ItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import jonius7.itemmover.network.PacketUpdateItemMover;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

@SideOnly(Side.CLIENT)
public class GuiItemMover extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("itemmover", "textures/gui/item_mover.png");
    private final TileEntityItemMover tile;

    public GuiItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        super(new ContainerItemMover(playerInv, tile));
        this.tile = tile;
        this.xSize = 256;
        this.ySize = 256;
    }
    
 // Call this whenever ghost slots change
    public void updateGhostSlots() {
        ContainerItemMover container = (ContainerItemMover) this.inventorySlots;
        for (int i = 0; i < container.ghostPullSlots.length; i++) {
            SlotGhost slot = container.ghostPullSlots[i];
            slot.putStack(tile.getGhostPull()[i]);
        }

        for (int i = 0; i < container.ghostPushSlots.length; i++) {
            SlotGhost slot = container.ghostPushSlots[i];
            slot.putStack(tile.getGhostPush()[i]);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // Pull + Push Slot Buttons
        int pullStartX = 26;
        int pullStartY = 50;
        int pushStartX = 153;
        int pushStartY = 50;
        int buttonWidth = 12;
        int buttonHeight = 10;
        int spacingX = 32;
        int spacingY = 20;

        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = guiLeft + pullStartX + col * spacingX;
            int y = guiTop + pullStartY + row * spacingY;

            GuiButton btn = new GuiButton(100 + i, x, y, buttonWidth, buttonHeight, "" + (i + 1));
            this.buttonList.add(btn);
        }
        
        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = guiLeft + pushStartX + col * spacingX;
            int y = guiTop + pushStartY + row * spacingY;

            GuiButton btn = new GuiButton(112 + i, x, y, buttonWidth, buttonHeight, "" + (i + 1));
            this.buttonList.add(btn);
        }
       
        // Side Selection Buttons
        this.buttonList.add(new GuiButton(0, guiLeft + 40, guiTop + 20, 60, 20, getSideName(tile.getInputSide())));
        this.buttonList.add(new GuiButton(1, guiLeft + 167, guiTop + 20, 60, 20, getSideName(tile.getOutputSide())));
        
        /*
        // --- Input Slot ---
        btnInputSlotUp = new GuiButton(0, guiLeft + 40, guiTop + 30, 20, 20, "+");
        btnInputSlotDown = new GuiButton(1, guiLeft + 60, guiTop + 30, 20, 20, "-");

        // --- Output Slot ---
        btnOutputSlotUp = new GuiButton(2, guiLeft + 40, guiTop + 60, 20, 20, "+");
        btnOutputSlotDown = new GuiButton(3, guiLeft + 60, guiTop + 60, 20, 20, "-");

        // --- Input Side ---
        btnInputSideUp = new GuiButton(4, guiLeft + 110, guiTop + 30, 20, 20, "+");
        btnInputSideDown = new GuiButton(5, guiLeft + 130, guiTop + 30, 20, 20, "-");

        // --- Output Side ---
        btnOutputSideUp = new GuiButton(6, guiLeft + 110, guiTop + 60, 20, 20, "+");
        btnOutputSideDown = new GuiButton(7, guiLeft + 130, guiTop + 60, 20, 20, "-");

        buttonList.add(btnInputSlotUp);
        buttonList.add(btnInputSlotDown);
        buttonList.add(btnOutputSlotUp);
        buttonList.add(btnOutputSlotDown);
        buttonList.add(btnInputSideUp);
        buttonList.add(btnInputSideDown);
        buttonList.add(btnOutputSideUp);
        buttonList.add(btnOutputSideDown);
        */
        
        /*//simple 3x3 grid
        for (int i = 0; i < 9; i++) {
        	int row = i / 3;
        	int col = i % 3;
        	
        	int x = guiLeft + 40 + col * 20;
        	int y = guiTop + 90 + row * 20;
        	GuiButton btn = new GuiButton(i + 8, x, y, 20, 20, "" + (i + 1));
        	buttonList.add(btn);
        }
        */
    }
    
    private String getSideName(int side) {
        return ForgeDirection.values()[side].name().toUpperCase();
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Check every button manually
        for (Object obj : this.buttonList) {
            GuiButton button = (GuiButton) obj;

            if (button.mousePressed(this.mc, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    // Left-click = default behavior
                    this.actionPerformed(button);
                    playButtonSound();
                } else if (mouseButton == 1) {
                    // Right-click = reverse logic
                    handleRightClickButton(button);
                    playButtonSound();
                }
                return; // stop processing further clicks
            }
        }

        // Let other GUI elements (like slots) handle clicks normally
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleRightClickButton(GuiButton button) {
        if (button.id == 0) {
            tile.cycleInputSide(false); // backwards
            button.displayString = getSideName(tile.getInputSide());
        } else if (button.id == 1) {
            tile.cycleOutputSide(false);
            button.displayString = getSideName(tile.getOutputSide());
        }
    }  
    
    /** Play the standard GUI button press sound (1.7.10-friendly) */
    private void playButtonSound() {
        try {
            // Common in 1.7.10 mappings:
            mc.getSoundHandler().playSound(
                PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)
            );
        } catch (Throwable t) {
            // Fallback if mappings differ or method missing: play a simple player sound
            if (mc.thePlayer != null) mc.thePlayer.playSound("random.click", 1.0F, 1.0F);
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
    	if (button.id == 0) {
            tile.cycleInputSide(true); // backwards
            button.displayString = getSideName(tile.getInputSide());
        } else if (button.id == 1) {
            tile.cycleOutputSide(true);
            button.displayString = getSideName(tile.getOutputSide());
        }
    	
    	ItemMover.network.sendToServer(new PacketUpdateItemMover(tile));
    	//ItemMover.network.sendToServer(new jonius7.itemmover.network.PacketUpdateItemMover(tile));
    	
        //super.mouseClicked(mouseX, mouseY, mouseButton);
    	
    	/*
        // Always fetch fresh values from the TileEntity
        int inputSlot = tile.getInputSlot();
        int outputSlot = tile.getOutputSlot();
        int inputSide = tile.getInputSide();
        int outputSide = tile.getOutputSide();

        switch (button.id) {
            case 0: inputSlot = Math.min(inputSlot + 1, tile.getMaxSlot(inputSide)); break;
            case 1: inputSlot = Math.max(0, inputSlot - 1); break;

            case 2: outputSlot = Math.min(outputSlot + 1, tile.getMaxSlot(outputSide)); break;
            case 3: outputSlot = Math.max(0, outputSlot - 1); break;

            case 4: inputSide = (inputSide + 1) % 6; break;
            case 5: inputSide = (inputSide + 5) % 6; break;

            case 6: outputSide = (outputSide + 1) % 6; break;
            case 7: outputSide = (outputSide + 5) % 6; break;
        }

        // Update TileEntity
        tile.setInputSlot(inputSlot);
        tile.setOutputSlot(outputSlot);
        tile.setInputSide(inputSide);
        tile.setOutputSide(outputSide);

        // Send update to server
        ItemMover.network.sendToServer(new jonius7.itemmover.network.PacketUpdateItemMover(tile));*/
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        fontRendererObj.drawString("Item Mover", 8, 6, 0x404040);

        /*
        // Always fetch fresh values from TileEntity
        fontRendererObj.drawString("Input Slot: " + tile.getInputSlot(), 40, 20, 0x404040);
        fontRendererObj.drawString("Output Slot: " + tile.getOutputSlot(), 40, 50, 0x404040);

        fontRendererObj.drawString("Input Side: " + TileEntityItemMover.getSideName(tile.getInputSide()), 110, 20, 0x404040);
        fontRendererObj.drawString("Output Side: " + TileEntityItemMover.getSideName(tile.getOutputSide()), 110, 50, 0x404040);
    	*/
        fontRendererObj.drawString("Pull", 10, 20, 0x404040);
        fontRendererObj.drawString("Push", 137, 20, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
