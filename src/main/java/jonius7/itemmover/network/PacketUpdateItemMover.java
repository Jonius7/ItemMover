package jonius7.itemmover.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketUpdateItemMover implements IMessage {

    private int x, y, z;
    private int slot;
    private int inputSide;
    private int outputSide;

    public PacketUpdateItemMover() {}

    public PacketUpdateItemMover(TileEntityItemMover te) {
        this.x = te.xCoord;
        this.y = te.yCoord;
        this.z = te.zCoord;
        this.slot = te.getTargetSlot();
        this.inputSide = te.getInputSide();
        this.outputSide = te.getOutputSide();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        slot = buf.readInt();
        inputSide = buf.readInt();
        outputSide = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(slot);
        buf.writeInt(inputSide);
        buf.writeInt(outputSide);
    }

    public static class Handler implements IMessageHandler<PacketUpdateItemMover, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateItemMover message, MessageContext ctx) {
            World world = ctx.getServerHandler().playerEntity.worldObj;
            TileEntity te = world.getTileEntity(message.x, message.y, message.z);
            if (te instanceof TileEntityItemMover) {
                TileEntityItemMover mover = (TileEntityItemMover) te;
                mover.setTargetSlot(message.slot);
                mover.setInputSide(message.inputSide);
                mover.setOutputSide(message.outputSide);
                mover.markDirty();
            }
            return null;
        }
    }
}
