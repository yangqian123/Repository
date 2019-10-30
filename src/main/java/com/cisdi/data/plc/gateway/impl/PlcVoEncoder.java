//package com.cisdi.data.plc.gateway.impl;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.cisdi.data.common.exception.BusinessException;
//import com.cisdi.data.sdk.enums.ByteOrder;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.MessageToByteEncoder;
//
//public class PlcVoEncoder extends MessageToByteEncoder<PlcVo> {
//	private ByteOrder byteOrder = ByteOrder.BIGENDIAN;
//
//	public PlcVoEncoder(ByteOrder byteOrder) {
//		this.byteOrder = byteOrder;
//	}
//
//	private static final Logger logger = LoggerFactory.getLogger(PlcVoEncoder.class);
//
//	private Object lockObj = new Object();
//
//	@Override
//	protected void encode(ChannelHandlerContext ctx, PlcVo msg, ByteBuf out) throws Exception {
//		try {
//			if(msg == null) {
//				throw new BusinessException("encode PlcVo is null");
//			}
//            synchronized (lockObj) {
//			    if(ByteOrder.LITTLEENDIAN.code().equals(byteOrder.code())) {
//			    	if(msg.getBody()==null){
//						out.writeIntLE(41);
//					}else {
//						out.writeIntLE(41 + msg.getBody().length);//长度 4
//					}
//			    	out.writeIntLE(msg.getMsgKey() == null ? 0 : Integer.valueOf(msg.getMsgKey()));// 电文Id 6
//			    	out.writeLongLE(0);//日期 8
//			    	out.writeLongLE(0);//时间 6
//			    	out.writeShort(msg.getSendDC()==null ? 0 : Integer.valueOf(msg.getSendDC()));//发送端主机的描述码 2
//                    out.writeIntLE(msg.getRevDC()==null ? 0 : Integer.valueOf(msg.getRevDC())); //接收端主机的描述码 2
//			    	out.writeIntLE(0); // 顺序号 4
//			    	out.writeLongLE(msg.getReserved());  // 保留字段 8
//					if(msg.getBody()==null){
//						out.writeByte(0);
//					}else {
//						out.writeBytes(msg.getBody());
//					}
//					out.writeByte(0);
//			    }else {
//					if(msg.getBody()==null){
//						out.writeInt(41);
//					}else {
//						out.writeInt(41 + msg.getBody().length);//长度 4
//
//					}
//                    out.writeInt(msg.getMsgKey() == null ? 0 : Integer.valueOf(msg.getMsgKey()));// 电文Id 6
//                    out.writeLong(0);//日期 8
//                    out.writeLong(0);//时间 6
//                    out.writeInt(msg.getSendDC()==null ? 0 : Integer.valueOf(msg.getSendDC()));//发送端主机的描述码 2
//                    out.writeInt(msg.getRevDC()==null ? 0 : Integer.valueOf(msg.getRevDC())); //接收端主机的描述码 2
//                    out.writeInt(0); // 顺序号 4
//                    out.writeLong(msg.getReserved());  // 保留字段 8
//					if(msg.getBody()==null){
//						out.writeByte(0);
//					}else {
//						out.writeBytes(msg.getBody());
//					}
//					out.writeByte(0);
//
//
//				}
//			}
//		} catch (Exception e) {
//			logger.warn("编码消息发生异常" + e.getMessage(), e);
//			throw e;
//		}
//	}
//}