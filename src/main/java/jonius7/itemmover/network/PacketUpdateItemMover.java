package jonius7.itemmover.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jonius7.itemmover.blocks.TileEntityItemMover;
import jonius7.itemmover.gui.GuiItemMover;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketUpdateItemMover implements IMessage {

    public int x, y, z;
    public int inputSide, outputSide;
    private ItemStack[] ghostPull;
    private ItemStack[] ghostPush;

    // --- Empty constructor required ---
    public PacketUpdateItemMover() {}

    // --- Construct packet from tile entity ---
    public PacketUpdateItemMover(TileEntityItemMover te) {
        this.x = te.xCoord;
        this.y = te.yCoord;
        this.z = te.zCoord;
        //this.inputSlot = te.getInputSlot();
        //this.outputSlot = te.getOutputSlot();
        this.inputSide = te.getInputSide();
        this.outputSide = te.getOutputSide();
        this.setGhostPull(te.getGhostPull());
        this.setGhostPush(te.getGhostPush());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        inputSide = buf.readInt();
        outputSide = buf.readInt();
        
        int pullLen = buf.readInt();
        setGhostPull(new ItemStack[pullLen]);
        for (int i = 0; i < pullLen; i++) {
            getGhostPull()[i] = ByteBufUtils.readItemStack(buf);
        }

        int pushLen = buf.readInt();
        setGhostPush(new ItemStack[pushLen]);
        for (int i = 0; i < pushLen; i++) {
            getGhostPush()[i] = ByteBufUtils.readItemStack(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(inputSide);
        buf.writeInt(outputSide);
        
        // Write pull ghost slots
        buf.writeInt(getGhostPull().length);
        for (ItemStack stack : getGhostPull()) {
            ByteBufUtils.writeItemStack(buf, stack);
        }

        // Write push ghost slots
        buf.writeInt(getGhostPush().length);
        for (ItemStack stack : getGhostPush()) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    /**
	 * @return the ghostPull
	 */
	public ItemStack[] getGhostPull() {
		return ghostPull;
	}

	/**
	 * @param ghostPull the ghostPull to set
	 */
	public void setGhostPull(ItemStack[] ghostPull) {
		this.ghostPull = ghostPull;
	}

	/**
	 * @return the ghostPush
	 */
	public ItemStack[] getGhostPush() {
		return ghostPush;
	}

	/**
	 * @param ghostPush the ghostPush to set
	 */
	public void setGhostPush(ItemStack[] ghostPush) {
		this.ghostPush = ghostPush;
	}

	public static class Handler implements IMessageHandler<PacketUpdateItemMover, IMessage> {
	    @Override
	    public IMessage onMessage(PacketUpdateItemMover message, MessageContext ctx) {
	    	// This runs on the server thread
	        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
	        if (player == null) return null;

	        World world = player.worldObj;
	        if (world == null) return null;

	        TileEntity te = world.getTileEntity(message.x, message.y, message.z);
	        if (te instanceof TileEntityItemMover) {
	            TileEntityItemMover mover = (TileEntityItemMover) te;

	            // Copy arrays to avoid reference issues
	            mover.setInputSide(message.getInputSide());
	            mover.setOutputSide(message.getOutputSide());
	            
	            mover.setGhostPull(message.getGhostPull());
	            mover.setGhostPush(message.getGhostPush());
	            mover.markDirty();
	            
	            // Notify clients so they update the block visually
	            world.markBlockForUpdate(mover.xCoord, mover.yCoord, mover.zCoord);
	        }

	        return null; // no response needed
	    }
	}
	
	public int getInputSide() { return inputSide; }
	public int getOutputSide() { return outputSide; }
}
