package jonius7.itemmover.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketSetSlotMapping implements IMessage {

    private int x, y, z;
    private boolean isPull;  // true = pull, false = push
    private int index;       // ghost slot index
    private int newValue;    // new mapped slot index

    // Empty constructor (required)
    public PacketSetSlotMapping() {}

    public PacketSetSlotMapping(TileEntityItemMover te, boolean isPull, int index, int newValue) {
        this.x = te.xCoord;
        this.y = te.yCoord;
        this.z = te.zCoord;
        this.isPull = isPull;
        this.index = index;
        this.newValue = newValue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        isPull = buf.readBoolean();
        index = buf.readInt();
        newValue = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(isPull);
        buf.writeInt(index);
        buf.writeInt(newValue);
    }

    public static class Handler implements IMessageHandler<PacketSetSlotMapping, IMessage> {
        @Override
        public IMessage onMessage(PacketSetSlotMapping message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;

            World world = player.worldObj;
            TileEntity te = world.getTileEntity(message.x, message.y, message.z);
            if (!(te instanceof TileEntityItemMover)) return null;

            TileEntityItemMover mover = (TileEntityItemMover) te;

            if (message.isPull) {
                mover.setPullSlotMapping(message.index, message.newValue);
            } else {
                mover.setPushSlotMapping(message.index, message.newValue);
            }

            mover.markDirty();
            world.markBlockForUpdate(message.x, message.y, message.z);
            return null;
        }
    }
}
