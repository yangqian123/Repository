package com.cisdi.data.plc.gateway.impl;

import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisdi.data.common.exception.BusinessException;
import com.cisdi.data.sdk.enums.ByteOrder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;

public class PlcVoEncoder extends MessageToByteEncoder<PlcVo> {
	private ByteOrder byteOrder = ByteOrder.BIGENDIAN;

	public PlcVoEncoder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	private static final Logger logger = LoggerFactory.getLogger(PlcVoEncoder.class);

	private Object lockObj = new Object();

	@Override
	protected void encode(ChannelHandlerContext ctx, PlcVo msg, ByteBuf out) throws Exception {
		try {
			if(msg == null) {
				throw new BusinessException("encode PlcVo is null");
			}
            synchronized (lockObj) {
			    if(ByteOrder.LITTLEENDIAN.code().equals(byteOrder.code())) {
				}
			}
		} catch (Exception e) {
			logger.warn("编码消息发生异常" + e.getMessage(), e);
			throw e;
		}
	}
    public static byte[] encodeInteger(Integer value, Charset charset, int fixedByteLength, byte prefix) {
        ByteBuf buf = Unpooled.buffer();
        try {
            byte[] bytes = String.valueOf(value).getBytes(charset);

            if(bytes.length > fixedByteLength) {
                throw new RuntimeException();
            }

            int leftByte = fixedByteLength - bytes.length;

            // 先填充头部
            for (int i = 0; i < leftByte; i++) {
                buf.writeByte(prefix);
            }
            buf.writeBytes(bytes);

            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            if(buf != null) {
                buf.release();
            }
        }
    }
    public static  byte[] encodeString(String value, Charset charset, int fixedByteLength, byte append) {
        ByteBuf buf = Unpooled.buffer();
        try {
            byte[] bytes = value.getBytes(charset);

            if(bytes.length > fixedByteLength) {
                throw new RuntimeException();
            }

            buf.writeBytes(bytes);

            int leftByte = fixedByteLength - bytes.length;

            for (int i = 0; i < leftByte; i++) {
                buf.writeByte(append);
            }

            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            if(buf != null) {
                buf.release();
            }
        }
    }
}