package jonius7.itemmover.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketTogglePushMode implements IMessage {

    private int x, y, z;

    public PacketTogglePushMode() {} // Required empty constructor

    public PacketTogglePushMode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    // --- Server-side handler ---
    public static class Handler implements IMessageHandler<PacketTogglePushMode, IMessage> {
        @Override
        public IMessage onMessage(PacketTogglePushMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;

            World world = player.worldObj;
            if (world == null) return null;

            TileEntity te = world.getTileEntity(message.x, message.y, message.z);
            if (te instanceof TileEntityItemMover) {
                TileEntityItemMover mover = (TileEntityItemMover) te;
                mover.setPushMode(!mover.getPushMode()); // ✅ toggle it
                mover.markDirty();
                world.markBlockForUpdate(message.x, message.y, message.z);
            }

            return null;
        }
    }
}
