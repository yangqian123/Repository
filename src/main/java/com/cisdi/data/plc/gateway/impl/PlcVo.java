package com.cisdi.data.plc.gateway.impl;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;

public class PlcVo implements Serializable {
	private static final long serialVersionUID = 1251102466675497863L;
	// 报文头
	private Integer length; // 长度
	private String msgKey; // 电文Id
	private String date;//日期
	private String time; //时间
	private String sendDC;//发送端主机的描述码
	private String revDC;//接收端主机的描述码
	private String seq;// 顺序号
	private String reserved; // 保留字段
	private String body;



	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public String getMsgKey() {
		return msgKey;
	}

	public void setMsgKey(String msgKey) {
		this.msgKey = msgKey;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getSendDC() {
		return sendDC;
	}

	public void setSendDC(String sendDC) {
		this.sendDC = sendDC;
	}

	public String getRevDC() {
		return revDC;
	}

	public void setRevDC(String revDC) {
		this.revDC = revDC;
	}

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public String getReserved() {
		return reserved;
	}

	public void setReserved(String reserved) {
		this.reserved = reserved;
	}

	@Override
    public String toString() {
    	String jsonString = JSON.toJSONString(this);
    	return jsonString;
    }
}
