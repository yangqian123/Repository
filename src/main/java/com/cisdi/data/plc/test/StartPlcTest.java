package com.cisdi.data.plc.test;
import com.cisdi.data.plc.gateway.PlcSocketGateway;
import com.cisdi.data.plc.gateway.impl.*;
import com.cisdi.data.sdk.enums.ByteOrder;
import com.cisdi.data.sdk.gateway.netty.SessionFactory;
import com.cisdi.data.sdk.gateway.netty.TcpIoService;
import com.cisdi.data.sdk.gateway.netty.impl.DefaultTcpIoService;
import com.cisdi.data.sdk.vo.GatewayVo;
import io.netty.handler.ssl.OpenSsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;


public class StartPlcTest {
	private static Logger logger = LoggerFactory.getLogger(PlcSocketGateway.class);


	private static TcpIoService ioService = null;
	@SuppressWarnings("unused")
	private static SessionFactory sessionFactory = null;

	public static void main(String[] args) {
		boolean available = OpenSsl.isAvailable();
		logger.info("openssl available={}", available);
		//集控20数据库
		String user="zjjk";
		String pwd="zjjk";
        String url="jdbc:oracle:thin:@10.11.11.20:1521:orcl";
		//公司数据库测试使用
//        String user="testzjjk";
//        String pwd="testzjjk";
//        String url="jdbc:oracle:thin:@10.73.9.80:1521:orcl";
		String parameter ="{\"listenIp\":\"0.0.0.0\",\"listenPort\":5002,\"threadSize\":50,\"timeout\":2000}";
		String driver="oracle.jdbc.driver.OracleDriver";
		String fun="A";//用于应答电文功能码为A
		String heartbeatkey="999999";//此版本未使用
		String jhykey="KYKW01";
		String yieldkey="KYKW02";
		String waterkey="KYKW03";
		DefaultTcpIoService defaultIoService = new DefaultTcpIoService();
		SessionFactory factory = new PlcSessionFactory(user,pwd,driver,url,fun,heartbeatkey,jhykey,yieldkey,waterkey);
		ServiceProviderTest testServiceProvider = new ServiceProviderTest();
		factory.init(testServiceProvider, null);
		PlcChannelInitializer<PlcVo> channelInitializer =
				new PlcChannelInitializer<PlcVo>(factory, ByteOrder.BIGENDIAN, null, null);
		GatewayVo instanceVo = new GatewayVo();
		instanceVo.setByteOrder(ByteOrder.BIGENDIAN);
		instanceVo.setParameter(parameter);
		defaultIoService.init(instanceVo, factory, channelInitializer);
		ioService = defaultIoService;
		sessionFactory = factory;
		boolean open = ioService.open();
		//定时清0任务
		TimerManage timerManage=new TimerManage();
		Calendar calendar=Calendar.getInstance();
		//定制任务每日2:00执行
		calendar.set(Calendar.HOUR_OF_DAY,2);
		calendar.set(Calendar.MINUTE,0);
		calendar.set(Calendar.SECOND,0);
		Date date=calendar.getTime();//第一次执行任务的时间
		if(date.before(new Date())){
			//如果第一张执行任务小于当前时间，要在第一次执行前加一天，以便在一下一个时间点执行，
			//如果不加就会马上执行。
			date=timerManage.addDay(date,1);
		}
		Timer timer=new Timer();
		NFDFlightDataTimerTask timertask =new NFDFlightDataTimerTask();
		timer.schedule(timertask,date,TimerManage.PERIOD_DAY);//启动定时清0定时器


		if(open == true) {
			AtomicBoolean shouldRun = new AtomicBoolean(true);
			AliveCheckTask task = new AliveCheckTask(120, shouldRun, factory);
			Thread thread = new Thread(task, "plc-alive-check-thread");
			thread.start();
			logger.info("plc网关启动成功");
		}else {
			logger.info("plc网关启动失败");
		}
		
		try {
			Thread.sleep(10000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
