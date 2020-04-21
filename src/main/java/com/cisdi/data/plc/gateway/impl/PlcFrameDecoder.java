package com.cisdi.data.plc.gateway.impl;

import java.util.List;

import com.cisdi.data.common.exception.BusinessException;
import com.cisdi.data.sdk.enums.ByteOrder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* <p>Description: 得到一个完整报文长度</p>
* @author
* @date 2019年8月12日
* @version 1.0
 */
public class PlcFrameDecoder extends ByteToMessageDecoder  {
	private ByteOrder byteOrder = ByteOrder.LITTLEENDIAN;
	private static final Logger logger = LoggerFactory.getLogger(PlcFrameDecoder.class);
	public PlcFrameDecoder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}
	
	// 30 byte 长度报文头
	/**
	 * 最小长度30
	 */
	private static final short Min_Length = 30;
	
	// 30 byte 长度，最大允许8192 Byte,防止攻击
	/**
	 * 修改Max_Length = 8192 + 30
	 */
	private static final short Max_Length = 8192 + 30;
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf bufferIn, List<Object> out) throws Exception {
//		int tmp=bufferIn.readableBytes();
//
//		if (bufferIn.readableBytes() < Min_Length) {
//	        return;
//	    }
//
//        final int beginIndex = bufferIn.readerIndex();
//
//        int length = 0;
//
//        if(ByteOrder.LITTLEENDIAN.code().equals(byteOrder.code())) {
//        	length = bufferIn.readIntLE();
//        }else {
//			length = bufferIn.readInt();
//		}
//
//        if(length > Max_Length) {
//            ctx.close();
//			throw new BusinessException("超过协议允许最大报文长度，默认为 " + Max_Length + " byte，当前为" + length + "byte");
//		}
//
//        if(length < Min_Length) {
//        	ctx.close();
//        	throw new BusinessException(ctx.channel().toString()+  "传输数据体长度小于41，长度字段必须为有符号short，此长度为:" + length);
//        }
//
//        if (bufferIn.readableBytes() < length - 4) { // 拆包
//            bufferIn.readerIndex(beginIndex);
//            return;
//        }
//
//        bufferIn.readerIndex(beginIndex);
//
//        ByteBuf otherByteBufRef = bufferIn.retainedSlice(beginIndex, length);
//
//        bufferIn.readerIndex(beginIndex + length);
//
//        out.add(otherByteBufRef);
//
//        // 处理可能的粘包
//		decode(ctx, bufferIn, out);
		if (bufferIn.readableBytes() <= Min_Length) {
			return;
		}

		final int beginIndex = bufferIn.readerIndex();
        /**
         * 修改报文长度字符数
         */
		byte[] headerBytes = new byte[4];
		bufferIn.readBytes(headerBytes);

		String header = new String(headerBytes, CharsetUtil.US_ASCII);

		// 解析headerString
        /**
         * 修改报文长度
         */
		String lendthString = getString(header, 0, 4);

		int length = 0;
		try {
			length=Integer.valueOf(lendthString);
		}catch (Exception e){
			logger.info("报文长度"+length+"不为整数");
		}


		if(length > Max_Length) {
			ctx.close();
			throw new BusinessException("超过协议允许最大报文长度，默认为 " + Max_Length + " byte，当前为" + length + "byte");
		}

		if(length < Min_Length) {
			ctx.close();
			throw new BusinessException(ctx.channel().toString()+  "传输数据体长度小于29，长度字段必须为有符号short，此长度为:" + length);
		}

        /**
         * 修改拆包长度：4
         */
		if (bufferIn.readableBytes() < length - 4) { // 拆包
			bufferIn.readerIndex(beginIndex);
			return;
		}

		bufferIn.readerIndex(beginIndex);

		ByteBuf otherByteBufRef = bufferIn.retainedSlice(beginIndex, length);

		bufferIn.readerIndex(beginIndex + length);

		out.add(otherByteBufRef);

		// 处理可能的粘包
		decode(ctx, bufferIn, out);
	}
	private String getString(String header, int start, int end) {
		char[] dst = new char[end - start];
		header.getChars(start, end, dst, 0);

		return new String(dst);
	}
}
