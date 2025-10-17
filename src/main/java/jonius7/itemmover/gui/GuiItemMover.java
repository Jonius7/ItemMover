package jonius7.itemmover.gui;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jonius7.itemmover.ItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import jonius7.itemmover.network.PacketSetSlotMapping;
import jonius7.itemmover.network.PacketTogglePushMode;
import jonius7.itemmover.network.PacketUpdateItemMover;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

@SideOnly(Side.CLIENT)
public class GuiItemMover extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("itemmover", "textures/gui/item_mover.png");
    private final TileEntityItemMover tile;
    private GuiButton buttonRequireSet;
    
    private final List<GuiButton> pullButtons = new ArrayList<>();
    private final List<GuiButton> pushButtons = new ArrayList<>();

    public GuiItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        super(new ContainerItemMover(playerInv, tile));
        this.tile = tile;
        this.xSize = 256;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.pullButtons.clear();
        this.pushButtons.clear();
        
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // Pull + Push Slot Buttons
        int pullStartX = 35;
        int pullStartY = 50;
        int pushStartX = 164;
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

            GuiButton btn = new GuiButton(100 + i, x, y, buttonWidth, buttonHeight, "" + (tile.getPullSlotMapping(i)));
            this.buttonList.add(btn);
            this.pullButtons.add(btn);
        }
        
        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = guiLeft + pushStartX + col * spacingX;
            int y = guiTop + pushStartY + row * spacingY;

            GuiButton btn = new GuiButton(112 + i, x, y, buttonWidth, buttonHeight, "" + (tile.getPushSlotMapping(i)));
            this.buttonList.add(btn);
            this.pushButtons.add(btn);
        }
       
        // Side Selection Buttons
        this.buttonList.add(new GuiButton(0, guiLeft + 50, guiTop + 19, 60, 20, getSideName(tile.getInputSide())));
        this.buttonList.add(new GuiButton(1, guiLeft + 179, guiTop + 19, 60, 20, getSideName(tile.getOutputSide())));
        
        // Smart Push button
        int requireX = guiLeft + 210;
        int requireY = guiTop + 173; // adjust position
        buttonRequireSet = new GuiButton(200, requireX, requireY, 80, 20,
            tile.getPushMode() ? "Smart Push: ON" : "Smart Push: OFF");
        buttonList.add(buttonRequireSet);
        
        tile.validateSlotMappings();
        refreshSlotButtons();
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
            tile.validateSlotMappings();
            refreshSlotButtons();
            button.displayString = getSideName(tile.getInputSide());
        } else if (button.id == 1) {
            tile.cycleOutputSide(false); // backwards
            tile.validateSlotMappings();
            refreshSlotButtons();
            button.displayString = getSideName(tile.getOutputSide());
        } else if (button.id >= 100 && button.id < 112) { // Pull slot buttons
            int ghostIndex = button.id - 100;
            IInventory inv = tile.getInputInventory();

            if (inv != null && inv.getSizeInventory() > 0) {
                int invSize = inv.getSizeInventory();
                int currentMapping = tile.getPullSlotMapping(ghostIndex);
                // Cycle backwards through available slots
                int nextMapping = (currentMapping - 1 + invSize) % invSize;

                tile.setPullSlotMapping(ghostIndex, nextMapping);
                button.displayString = String.valueOf(nextMapping);

                // Send to server
                ItemMover.network.sendToServer(new PacketSetSlotMapping(tile, true, ghostIndex, nextMapping));
            } else {
                button.enabled = false;
            }
        } else if (button.id >= 112 && button.id < 124) { // Push slot buttons
            int ghostIndex = button.id - 112;
            IInventory inv = tile.getOutputInventory();

            if (inv != null && inv.getSizeInventory() > 0) {
                int invSize = inv.getSizeInventory();
                int currentMapping = tile.getPushSlotMapping(ghostIndex);
                // Cycle backwards through available slots
                int nextMapping = (currentMapping - 1 + invSize) % invSize;

                tile.setPushSlotMapping(ghostIndex, nextMapping);
                button.displayString = String.valueOf(nextMapping);

                // Send to server
                ItemMover.network.sendToServer(new PacketSetSlotMapping(tile, false, ghostIndex, nextMapping));
            } else {
                button.enabled = false;
            }
        }
        ItemMover.network.sendToServer(new PacketUpdateItemMover(tile));
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
    	if (button.id == 0) {
            tile.cycleInputSide(true); // forwards
            tile.validateSlotMappings();
            refreshSlotButtons();
            button.displayString = getSideName(tile.getInputSide());
        } else if (button.id == 1) {
            tile.cycleOutputSide(true); // forwards
            tile.validateSlotMappings();
            refreshSlotButtons();
            button.displayString = getSideName(tile.getOutputSide());
        } else if (button.id == 200) {
            boolean newState = !tile.getPushMode();
            tile.setPushMode(newState);
            button.displayString = newState ? "Smart Push: ON" : "Smart Push: OFF";
            ItemMover.network.sendToServer(
                new PacketTogglePushMode(tile.xCoord, tile.yCoord, tile.zCoord)
            );
            return;
        } else if (button.id >= 100 && button.id < 112) { // Pull slot buttons
            int ghostIndex = button.id - 100;
            IInventory inv = tile.getInputInventory();

            if (inv != null && inv.getSizeInventory() > 0) {
                int currentMapping = tile.getPullSlotMapping(ghostIndex);
                int nextMapping = (currentMapping + 1) % inv.getSizeInventory();
                tile.setPullSlotMapping(ghostIndex, nextMapping);
                button.displayString = String.valueOf(nextMapping);

                // Send update to server
                ItemMover.network.sendToServer(new PacketSetSlotMapping(tile, true, ghostIndex, nextMapping));
            } else {
                button.enabled = false;
            }
        } else if (button.id >= 112 && button.id < 124) { // Push slot buttons
            int ghostIndex = button.id - 112;
            IInventory inv = tile.getOutputInventory();

            if (inv != null && inv.getSizeInventory() > 0) {
                int currentMapping = tile.getPushSlotMapping(ghostIndex);
                int nextMapping = (currentMapping + 1) % inv.getSizeInventory();
                tile.setPushSlotMapping(ghostIndex, nextMapping);
                button.displayString = String.valueOf(nextMapping);

                // Send update to server
                ItemMover.network.sendToServer(new PacketSetSlotMapping(tile, false, ghostIndex, nextMapping));
            } else {
                button.enabled = false;
            }
        }
    	ItemMover.network.sendToServer(new PacketUpdateItemMover(tile));
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
    
    /**
     * Refreshes all pull/push slot buttons to reflect the connected inventories.
     * Updates their display text and disables buttons for invalid slots.
     */
    public void refreshSlotButtons() {
        // --- PULL BUTTONS ---
        IInventory inputInv = tile.getInputInventory();
        int inputSize = (inputInv != null) ? inputInv.getSizeInventory() : 0;

        int pullCount = Math.min(pullButtons.size(), tile.getPullMappingLength());
        for (int i = 0; i < pullButtons.size(); i++) {
            GuiButton button = pullButtons.get(i);
            if (i < pullCount && inputSize > 0 && i < inputSize) {
                button.displayString = String.valueOf(tile.getPullSlotMapping(i));
                button.enabled = true;
            } else {
                button.displayString = "-";
                button.enabled = false;
            }
        }

        // --- PUSH BUTTONS ---
        IInventory outputInv = tile.getOutputInventory();
        int outputSize = (outputInv != null) ? outputInv.getSizeInventory() : 0;

        int pushCount = Math.min(pushButtons.size(), tile.getPushMappingLength());
        for (int i = 0; i < pushButtons.size(); i++) {
            GuiButton button = pushButtons.get(i);
            if (i < pushCount && outputSize > 0 && i < outputSize) {
                button.displayString = String.valueOf(tile.getPushSlotMapping(i));
                button.enabled = true;
            } else {
                button.displayString = "-";
                button.enabled = false;
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        fontRendererObj.drawString("Item Mover", 8, 6, 0x404040);
        fontRendererObj.drawString("Pull", 22, 25, 0x404040);
        fontRendererObj.drawString("Push", 149, 25, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
