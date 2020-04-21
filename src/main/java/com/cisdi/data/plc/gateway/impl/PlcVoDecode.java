package com.cisdi.data.plc.gateway.impl;

import com.cisdi.data.sdk.enums.ByteOrder;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.apache.tomcat.util.buf.HexUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 
* <p>Title: StandardVoDecode.java</p>
* <p>Description: 将报文做下一步分解</p>
* @author
* @date 2019年3月22日
* @version 1.0
 */
public class PlcVoDecode {
	public static PlcVo Decode(ByteBuf in, ByteOrder byteOrder) {
        /**
         * 根据宝能29号规约，报文头的属性长度29
         */
		byte[] headerBytes = new byte[29];
		in.readBytes(headerBytes);

		String header = new String(headerBytes, CharsetUtil.US_ASCII);
		PlcVo result = new PlcVo();
		int length = 0;

			//int length = in.readIntLE();
//			String length=null;
//			for(int i=0;i<4;i++){
//				length+=in.readChar();
//			}
//			String msgKey=null;
//			for(int i=0;i<6;i++)
//			{
//				msgKey+=in.readChar();
//			}
//			//long date=in.readLongLE();
//			String date=null;
//			for(int i=0;i<8;i++){
//				date+=in.readChar();
//			}
//			String time=null;
//			for(int i=0;i<6;i++)
//			{
//				time+=in.readChar();
//			}
//			String sendDC=null;
//			for(int i=0;i<2;i++){
//				sendDC+=in.readChar();
//			}
//			String revDC=null;
//			for(int i=0;i<2;i++){
//				revDC+=in.readChar();
//			}
//
//			//int seq = in.readIntLE();
//			String seq=null;
//			for(int i=0;i<4;i++){
//				seq+=in.readChar();
//			}
//			//long reserved = in.readLongLE();
//			String reserved=null;
//			for(int i=0;i<8;i++){
//				reserved+=in.readChar();
//			}
//
//			result.setLength(length);
//			result.setMsgKey(msgKey);
//			result.setSeq(seq);
//			result.setReserved(reserved);
//			result.setRevDC(revDC);
//			result.setSendDC(sendDC);
//			result.setTime(time);
//			result.setDate(date);
			// 解析headerString

        /**
         * 根据宝能29号规约，修改报文头的属性长度
         */
			String lendthString = getString(header, 0, 4);
			String msgKey = getString(header, 4, 10);
			String date = getString(header, 10, 18);
			String time = getString(header, 18, 24);
			String sendDc = getString(header, 24, 26);
			String revDC = getString(header, 26, 28);
			String fun = getString(header, 28, 29);
			length = Integer.valueOf(lendthString);
			result.setLength(length);
			result.setMsgKey(msgKey);
			result.setDate(date);
			result.setTime(time);
			result.setSendDC(sendDc);
			result.setRevDC(revDC);
			result.setFun(fun);
			if (length > 30) {
				int bodyLength = length - 30;
				byte[] bodyBytes = new byte[bodyLength];
				in.readBytes(bodyBytes);
				String body = new String(bodyBytes, CharsetUtil.US_ASCII);
				result.setBody(body);
			}

			// 读取结束符
			in.readByte();

			return result;

	}
	private static String getString(String header, int start, int end) {
		char[] dst = new char[end - start];
		header.getChars(start, end, dst, 0);

		return new String(dst);
	}

}
