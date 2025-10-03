package jonius7.itemmover.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jonius7.itemmover.ItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class GuiItemMover extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("itemmover", "textures/gui/item_mover.png");
    private final TileEntityItemMover tile;

    private GuiButton btnInputSlotUp, btnInputSlotDown;
    private GuiButton btnOutputSlotUp, btnOutputSlotDown;
    private GuiButton btnInputSideUp, btnInputSideDown;
    private GuiButton btnOutputSideUp, btnOutputSideDown;

    public GuiItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        super(new ContainerItemMover(playerInv, tile));
        this.tile = tile;
        this.xSize = 256;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        
        int startX = 26; // top-left X for grid
        int startY = 35; // top-left Y for grid
        int buttonWidth = 12;
        int buttonHeight = 10;
        int spacing = 32;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = guiLeft + startX + col * spacing;
            int y = guiTop + startY + row * spacing;

            GuiButton btn = new GuiButton(100 + i, x, y, buttonWidth, buttonHeight, "" + (i + 1));
            this.buttonList.add(btn);
        }
        
        
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

    @Override
    protected void actionPerformed(GuiButton button) {
    	int id = button.id;
        if (id >= 100 && id < 109) {
            int slotIndex = id - 100;
            // Do something with the ghost slot at slotIndex
            //tile.cycleSideForSlot(slotIndex); // implement logic in TileEntity
        }
    	
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
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
