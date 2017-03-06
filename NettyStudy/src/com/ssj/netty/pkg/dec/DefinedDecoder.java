package com.ssj.netty.pkg.dec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class DefinedDecoder extends ByteToMessageDecoder {

	String content="";
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		byte[] req=new byte[in.readableBytes()];
		String str=new String(req);
		content=content+str;
		int index=content.indexOf(System.getProperty("line.separator"));
		if(index==-1){
			return;
		}
		String name=content.substring(0,index+1);
		out.add(name);
	}
	
}
