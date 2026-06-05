package jonius7.itemmover.client.render;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import jonius7.itemmover.blocks.BlockItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.IIcon;

public class RenderItemMover implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RenderItemMover(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        // Render base block only for inventory
        Tessellator tessellator = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(0, metadata));
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(1, metadata));
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(2, metadata));
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(3, metadata));
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(4, metadata));
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(5, metadata));
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        // 1. Base Layer
        renderer.renderStandardBlock(block, x, y, z);

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityItemMover) {
            TileEntityItemMover mover = (TileEntityItemMover) te;
            BlockItemMover moverBlock = (BlockItemMover) block;

            // 2. Second Layer: Color/Side Indicator (Offset 0.001)
            drawFace(renderer, block, x, y, z, mover.getInputSide(), moverBlock.getSideIcon(mover.getInputSide()), 0.001D);
            drawFace(renderer, block, x, y, z, mover.getOutputSide(), moverBlock.getSideIcon(mover.getOutputSide()), 0.001D);

            // 3. Third Layer: Push/Pull Icon (Offset 0.002)
            drawFace(renderer, block, x, y, z, mover.getInputSide(), moverBlock.getPullIcon(), 0.002D);
            drawFace(renderer, block, x, y, z, mover.getOutputSide(), moverBlock.getPushIcon(), 0.002D);
        }
        return true;
    }

    private void drawFace(RenderBlocks renderer, Block block, int x, int y, int z, int side, IIcon icon, double offset) {
        if (icon == null) return;
        
        // 1. Calculate the bounds based on the face and the offset
        // We expand the bounds slightly to "float" the sticker just above the base face
        if (side == 0) { // Bottom
            renderer.renderMinY = -offset; renderer.renderMaxY = offset;
        } else if (side == 1) { // Top
            renderer.renderMinY = 1.0 - offset; renderer.renderMaxY = 1.0 + offset;
        } else if (side == 2) { // North (Z-)
            renderer.renderMinZ = -offset; renderer.renderMaxZ = offset;
        } else if (side == 3) { // South (Z+)
            renderer.renderMinZ = 1.0 - offset; renderer.renderMaxZ = 1.0 + offset;
        } else if (side == 4) { // West (X-)
            renderer.renderMinX = -offset; renderer.renderMaxX = offset;
        } else if (side == 5) { // East (X+)
            renderer.renderMinX = 1.0 - offset; renderer.renderMaxX = 1.0 + offset;
        }

        // 2. Set the texture and render the face
        renderer.setOverrideBlockTexture(icon);
        
        if (side == 0) renderer.renderFaceYNeg(block, x, y, z, icon);
        else if (side == 1) renderer.renderFaceYPos(block, x, y, z, icon);
        else if (side == 2) renderer.renderFaceZNeg(block, x, y, z, icon);
        else if (side == 3) renderer.renderFaceZPos(block, x, y, z, icon);
        else if (side == 4) renderer.renderFaceXNeg(block, x, y, z, icon);
        else if (side == 5) renderer.renderFaceXPos(block, x, y, z, icon);
        
        // 3. IMPORTANT: Reset and Cleanup
        renderer.clearOverrideBlockTexture();
        
        // Reset bounds back to full block so other rendering calls aren't broken
        renderer.renderMinX = 0.0; renderer.renderMaxX = 1.0;
        renderer.renderMinY = 0.0; renderer.renderMaxY = 1.0;
        renderer.renderMinZ = 0.0; renderer.renderMaxZ = 1.0;
    }
    
    @Override
    public int getRenderId() {
        return this.renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }
}