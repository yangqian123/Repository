package com.cisdi.data.plc.gateway.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisdi.data.sdk.enums.ByteOrder;
import com.cisdi.data.sdk.gateway.netty.SessionFactory;
import com.cisdi.data.sdk.gateway.netty.impl.AbstractChannelInitializer;
import com.cisdi.data.sdk.gateway.netty.impl.AbstractIoChannelHandler;
import com.cisdi.data.sdk.gateway.netty.impl.DefaultIoChannelHandler;
import com.cisdi.data.sdk.gateway.netty.impl.PassThroughChannelHandler;
import com.cisdi.data.sdk.utils.NettySslUtils;
import com.cisdi.data.sdk.vo.SslConfigVo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class PlcChannelInitializer<I> extends AbstractChannelInitializer<I> {

	private static final Logger logger = LoggerFactory.getLogger(PlcChannelInitializer.class);
	
	private SessionFactory factory;
	private ByteOrder byteOrder;
	private String passthroughAddress;
	private SslConfigVo sslConfigVo;
	
	/**
	 * 构造通道初始化器对象
	 * @param factory 会话工厂
	 * @param byteOrder 大小端
	 * @param passthroughAddress 透传地址，如果不使用透传，传null
	 * @param sslConfig ssl证书配置，如果不启用tls，传null
	 */
	public PlcChannelInitializer(SessionFactory factory, 
			ByteOrder byteOrder, 
			String passthroughAddress,
			SslConfigVo sslConfig) {
		this.factory = factory;
		this.byteOrder = byteOrder;
		this.passthroughAddress = passthroughAddress;
		this.sslConfigVo = sslConfig;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected AbstractIoChannelHandler<I> getTailHandler() {
		return new DefaultIoChannelHandler(factory);
	}

	@Override
	protected List<ChannelHandler> getHandlers() {
		List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
		handlers.add(new PlcFrameDecoder(byteOrder));
		
		if(StringUtils.isNotEmpty(passthroughAddress)) {
			handlers.add(new PassThroughChannelHandler(passthroughAddress));
		}
		
		//handlers.add(new PlcVoEncoder(byteOrder));
		return handlers;
	}

	@Override
	protected IdleStateHandler getIdleStateHandler() {
		return new IdleStateHandler(15, 15, 30);
	}

	@Override
	protected ChannelHandler getSslChannelHandler(Channel socketChannel) {
		ChannelHandler sslHandler = null;
		try {
		    if(sslConfigVo != null) {
		    	sslHandler = NettySslUtils.getServerSslHandler((SocketChannel)socketChannel, sslConfigVo);
		    }
		} catch (Exception e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
		return sslHandler;
	}

}
