package com.cisdi.data.plc.gateway;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.cisdi.data.common.exception.BusinessException;
import com.cisdi.data.plc.gateway.impl.OtherParameterVo;
import com.cisdi.data.plc.gateway.impl.PlcChannelInitializer;
import com.cisdi.data.plc.gateway.impl.PlcVo;
import com.cisdi.data.plc.gateway.impl.PlcSessionFactory;
import com.cisdi.data.sdk.consts.ServiceName;
import com.cisdi.data.sdk.enums.GatewayState;
import com.cisdi.data.sdk.gateway.base.SocketGatewayBase;
import com.cisdi.data.sdk.gateway.netty.IoSession;
import com.cisdi.data.sdk.gateway.netty.SessionFactory;
import com.cisdi.data.sdk.gateway.netty.TcpIoService;
import com.cisdi.data.sdk.gateway.netty.impl.DefaultTcpIoService;
import com.cisdi.data.sdk.param.ListenSocketParam;
import com.cisdi.data.sdk.procotol.message.SocketReturnMessage;
import com.cisdi.data.sdk.service.PlatformService;
import com.cisdi.data.sdk.vo.ExeResultVo;
import com.cisdi.data.sdk.vo.SslConfigVo;


public class PlcSocketGateway extends SocketGatewayBase {

	private static Logger logger = LoggerFactory.getLogger(PlcSocketGateway.class);

	TcpIoService ioService = null;
	SessionFactory sessionFactory = null;

	@Override
	public ExeResultVo sendReturnMessage(SocketReturnMessage returnMsg) {
		ExeResultVo resultVo = new ExeResultVo();

		try {
			IoSession session = null;
			List<IoSession> sessions = sessionFactory.getSessions();

			for (IoSession ioSession : sessions) {
				if(ArrayUtils.indexOf(ioSession.getDeviceIds(), returnMsg.getDeviceId(), 0) > -1) {
					session = ioSession;
					break;
				}
			}

		    if(session == null) {
		    	logger.warn("plc网关:{}该会话不存在,消息未正常返回:{}",instanceVo.getRunId(), returnMsg);

		    	resultVo.setSuccess(false);
		    	resultVo.setMessage(String.format("和设备:%s的会话不存在", returnMsg.getDeviceId()));
		    }else {
		    	PlcVo vo = new PlcVo();
		    //	vo.setDeviceId(returnMsg.getDeviceId());
		    //	vo.setBody(returnMsg.getData());
		    	vo.setMsgKey(returnMsg.getMsgKey());

		    	session.send(vo);
		    	resultVo.setSuccess(true);
		    }
		} catch (BusinessException businessException) {
			logger.info("plc网关:{}发送消息给设备发生业务异常:{}",instanceVo.getRunId(), businessException);
			resultVo.setSuccess(false);
			resultVo.setMessage(businessException.getMessage());
		}
		catch (Exception e) {
			logger.warn("plc网关:{}发送消息给设备发生系统异常:{}",instanceVo.getRunId(), e);
			resultVo.setSuccess(false);
			resultVo.setMessage(e.getMessage());
		}

		logger.info("plc网关:{}->设备，消息:{},结果:{}",instanceVo.getRunId(), returnMsg, resultVo);

		return resultVo;
		//return  null;

	}

	@Override
	public void start() {
		if(state == GatewayState.RUNNING) {
			return;
		}

		logger.info("plc网关:{}读取启动配置参数:{}",instanceVo.getRunId(), getInstanceVo().getParameter());

		DefaultTcpIoService defaultIoService = new DefaultTcpIoService();
		SessionFactory factory = new PlcSessionFactory();
		factory.init(serviceProvider, this);

		String passthroughAddress = null;

		if(getInstanceVo().getNeedPassthrough() != null && Boolean.TRUE.equals(getInstanceVo().getNeedPassthrough())) {
			passthroughAddress = getInstanceVo().getPassthroughAddress();
		}

		SslConfigVo sslConfigVo = null;

		ListenSocketParam socketParam = JSON.parseObject(getInstanceVo().getParameter(), ListenSocketParam.class);

		if(StringUtils.isNotEmpty(socketParam.getOtherParameter())) {
			String paramString = socketParam.getOtherParameter();

			OtherParameterVo otherVo = JSON.parseObject(paramString, OtherParameterVo.class);

			if(otherVo != null && Boolean.TRUE.equals(otherVo.getEnableSsl())) {

				PlatformService platformService = (PlatformService)serviceProvider.getByName(ServiceName.Platform);

				sslConfigVo = platformService.getSslConfig();
			}
		}

		PlcChannelInitializer<PlcVo> channelInitializer =
				new PlcChannelInitializer<PlcVo>(factory, getInstanceVo().getByteOrder(), passthroughAddress, sslConfigVo);

		defaultIoService.init(instanceVo, factory, channelInitializer);

		ioService = defaultIoService;
		sessionFactory = factory;

		boolean open = ioService.open();

		if(open == true) {
			state = GatewayState.RUNNING;
			logger.info("plc网关:{}启动成功 参数:{}", instanceVo.getRunId(), getInstanceVo().getParameter());
		}else {
			logger.info("plc网关:{}启动失败 参数:{}", instanceVo.getRunId(), getInstanceVo().getParameter());
		}
	}

	@Override
	public void shutdown() {
		if(state == GatewayState.CLOSED) {
			return;
		}

		if(sessionFactory != null) {
        	for (IoSession ioSession : sessionFactory.getSessions()) {
        		try {
        			ioSession.getChannel().close();
				} catch (Exception e) {
					logger.warn(e.getLocalizedMessage(), e);
				}
        	}
		}

		boolean close = ioService.close();
		if(close == true) {
			sessionFactory = null;
			state = GatewayState.CLOSED;
			logger.info("plc网关:{}关闭成功", instanceVo.getRunId());
		}else {
			logger.info("plc网关:{}关闭失败", instanceVo.getRunId());
		}
	}

	@Override
	public Set<String> getActiveDeviceIds() {
		return null;
	}


//	@Override
//    public Set<String> getActiveDeviceIds(){
//		activeDeviceIds.clear();
//
//        if(sessionFactory != null) {
//        	for (IoSession ioSession : sessionFactory.getSessions()) {
//        		String[] deviceIds = ioSession.getDeviceIds();
//        		for (int i = 0; i < deviceIds.length; i++) {
//    				activeDeviceIds.add(deviceIds[i]);
//				}
//        	}
//        }
//
//		return activeDeviceIds;
//	}
}
