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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HTTPHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger logger = Logger.getLogger(HTTPHandler.class.getName());
    private final static HashMap<String, String> mimeTypes = new HashMap<String, String>();

    static {
        mimeTypes.put("html", "text/html");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("css", "text/css");
    }

    private final int port;

    public HTTPHandler(int port) {
        this.port = port;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest msg)
            throws Exception {
        httpRequest(ctx, msg);
    }

    public void httpRequest(ChannelHandlerContext context, FullHttpRequest request) throws IOException {
        if (!request.getDecoderResult().isSuccess()) {
            sendHttpResponse(context, request, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }
        if (request.getUri().equals("/server")) {
            context.fireChannelRead(request);
            return;
        }

        if ((request.getMethod() == OPTIONS || request.getMethod() == HEAD)
                && request.getUri().equals("/chunk")) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            response.headers().add("Access-Control-Allow-Origin", "*");
            response.headers().add("Access-Control-Allow-Methods", "POST");
            if (request.getMethod() == OPTIONS) {
                response.headers().add("Access-Control-Allow-Headers", "origin, content-type, accept");
            }
            sendHttpResponse(context, request, response);
        }

        if (request.getMethod() != GET) {
            sendHttpResponse(context, request, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        // TODO: send browserified page
        if (request.getUri().equals("/")) {
            request.setUri("/index.html");
        }

        InputStream stream = null;
        /*
        if (request.getUri().startsWith("/resources/")) {
            File file = new File(
                    plugin.getResourceDir(),
                    request.getUri().substring("/resources/".length())
            );
            stream = new FileInputStream(file);
        } else {
        */
        stream = this.getClass().getClassLoader()
                .getResourceAsStream("www" +
                        request.getUri());
        if (stream == null) {
            sendHttpResponse(context, request, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            return;
        }
        ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());
        IOUtils.copy(stream, out);
        stream.close();
        out.close();

        ByteBuf buffer = out.buffer();
        if (request.getUri().equals("/index.html")) {
            String page = buffer.toString(Charsets.UTF_8);
            page = page.replaceAll("%SERVERPORT%", Integer.toString(this.port));
            buffer.release();
            buffer = Unpooled.wrappedBuffer(page.getBytes(Charsets.UTF_8));
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buffer);
        if (request.getUri().startsWith("/resources/")) {
            response.headers().add("Access-Control-Allow-Origin", "*");
        }

        String ext = request.getUri().substring(request.getUri().lastIndexOf('.') + 1);
        String type = mimeTypes.containsKey(ext) ? mimeTypes.get(ext) : "text/plain";
        if (type.startsWith("text/")) {
            type += "; charset=UTF-8";
        }
        response.headers().set(CONTENT_TYPE, type);
        setContentLength(response, response.content().readableBytes());
        sendHttpResponse(context, request, response);

    }

    public void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest request, FullHttpResponse response) {
        if (response.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        setContentLength(response, response.content().readableBytes());

        ChannelFuture future = context.channel().writeAndFlush(response);
        if (!isKeepAlive(request) || response.getStatus().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
