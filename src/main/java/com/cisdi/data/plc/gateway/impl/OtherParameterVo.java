package com.cisdi.data.plc.gateway.impl;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;

public class OtherParameterVo implements Serializable {
	private static final long serialVersionUID = 2247503397521281192L;
	
	private Boolean enableSsl;

	public Boolean getEnableSsl() {
		return enableSsl;
	}

	public void setEnableSsl(Boolean enableSsl) {
		this.enableSsl = enableSsl;
	}
	
	@Override
    public String toString() {
    	String jsonString = JSON.toJSONString(this);
    	return jsonString;
    }
}
