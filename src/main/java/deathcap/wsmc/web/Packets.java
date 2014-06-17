/*
 * Copyright 2014 Matthew Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package deathcap.wsmc.web;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Packets {

    public static ByteBuf writeSpawnPosition(int x, int y, int z) {
        ByteBuf buf = Unpooled.buffer(1 + 4 + 1 + 4);
        buf.writeByte(1);
        buf.writeInt(x);
        buf.writeByte(y);
        buf.writeInt(z);
        return buf;
    }

    public static ByteBuf writeTimeUpdate(int time) {
        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(0);
        buf.writeInt(time);
        return buf;
    }

    public static ByteBuf writeChatMessage(String msg) {
        byte[] msgBytes = msg.getBytes(Charsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(1 + msgBytes.length);
        buf.writeByte(2);
        buf.writeBytes(msgBytes);
        return buf;
    }
}
