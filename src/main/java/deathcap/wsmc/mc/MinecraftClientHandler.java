package deathcap.wsmc.mc;

import com.flowpowered.networking.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;

public class MinecraftClientHandler extends ChannelHandlerAdapter {

    public static final int HANDSHAKE_OPCODE = 0;
    public static final int MC_PROTOCOL_VERSION = 5; // 1.7.9
    public static final int NEXT_STATE_LOGIN = 2;

    // http://wiki.vg/Protocol#Clientbound_3
    public static final int LOGIN_DISCONNECT_OPCODE = 0;
    public static final int LOGIN_ENCRYPTION_REQUEST_OPCODE  = 1;
    public static final int LOGIN_SUCCESS_OPCODE = 2;

    public static final int LOGIN_OPCODE = 0;

    private boolean loggingIn = true;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf m = (ByteBuf) msg;

        try {
            int opcode = ByteBufUtils.readVarInt(m);
            System.out.println("opcode = " + opcode);
            if (loggingIn) {
                if (opcode == LOGIN_DISCONNECT_OPCODE) {
                    String reason = ByteBufUtils.readUTF8(m);
                    System.out.println("disconnect reason = " + reason);
                } else if (opcode == LOGIN_ENCRYPTION_REQUEST_OPCODE) {
                    System.out.println("encryption request!");
                    ctx.close();
                } else if (opcode == LOGIN_SUCCESS_OPCODE) {
                    System.out.println("login success");
                } else {
                    System.out.println("?? unrecognized opcode: "+opcode);
                }
                loggingIn = false;
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        System.out.println("Connected to "+ctx.channel().remoteAddress());

        String address = "localhost";
        int port = 25565;
        String username = "test";

        ByteBuf handshake = Unpooled.buffer();
        ByteBufUtils.writeVarInt(handshake, HANDSHAKE_OPCODE);
        ByteBufUtils.writeVarInt(handshake, MC_PROTOCOL_VERSION);

        ByteBufUtils.writeVarInt(handshake, address.length());
        handshake.writeBytes(address.getBytes());
        handshake.writeShort(port);
        ByteBufUtils.writeVarInt(handshake, NEXT_STATE_LOGIN);

        final ChannelFuture f = ctx.writeAndFlush(handshake);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                System.out.println("wrote handshake packet");
                assert f == channelFuture;
            }
        });

        ByteBuf login = Unpooled.buffer();
        ByteBufUtils.writeVarInt(login, LOGIN_OPCODE);
        try {
            ByteBufUtils.writeUTF8(login, username);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("exception writing username");
        }

        ctx.writeAndFlush(login);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Disconnected from "+ctx.channel().remoteAddress());
    }
}
