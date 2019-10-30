package com.cisdi.data.plc.gateway.impl;
import com.cisdi.data.sdk.gateway.base.SocketGatewayBase;
import com.cisdi.data.sdk.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cisdi.data.sdk.consts.ServiceName;
import com.cisdi.data.sdk.enums.ByteOrder;
import com.cisdi.data.sdk.gateway.message.SocketMessage;
import com.cisdi.data.sdk.gateway.netty.IoSession;
import com.cisdi.data.sdk.gateway.netty.impl.AbstractIoSession;
import com.cisdi.data.sdk.service.SendService;
import io.netty.buffer.ByteBuf;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 
* <p>Title: StandardIoSession.java</p>
* <p>Description: 标准Socket协议会话实现</p>
* @author
* @date 2019年8月12日
* @version 1.0
 */

public class PlcIoSession extends AbstractIoSession implements IoSession {
	private PlcSessionFactory factory ;

	OracleReader oracle=null;
	private static Logger logger = LoggerFactory.getLogger(PlcIoSession.class);
    SimpleDateFormat formatime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private long lastAliveTime = System.currentTimeMillis();
	private String gwPrefixCache = null;

	@Override
	public void init(String id, ServiceProvider provider, SocketGatewayBase socketGateway) {
		this.id = id;
		this.serviceProvider = provider;
		this.socketGateway = socketGateway;
		oracle=new OracleReader(factory.getUser(),factory.getPwd(),factory.getDriver(),factory.getUrl());
		oracle.connection();

	}
	@Override
	public void onRead(Object message) {
		ByteOrder byteOrder = socketGateway == null ? ByteOrder.BIGENDIAN : socketGateway.getInstanceVo().getByteOrder();
		PlcVo vo = PlcVoDecode.Decode((ByteBuf)message, byteOrder);
        if(vo != null && serviceProvider != null && socketGateway != null) {
        	SendService service = (SendService)serviceProvider.getByName(ServiceName.Send);
    		SocketMessage socketMessage = socketGateway.buildSocketMessage();
    		socketMessage.setDeviceId(vo.getSendDC()+"");
    		//socketMessage.setData(vo.getBody());
    		socketMessage.setMsgKey(vo.getMsgKey());
    		service.sendMessage(socketMessage);
        }else {
        	//电文长度
			int lengths=vo.getBody().length();
			//电文数据体
			String hexString=vo.getBody();
            Date nowDate=new Date();
			 //查询数据，写入数据。
			if(vo.getMsgKey().equals(factory.getJhykey())){
				//输出电文信息，电文长度，内容，条数
				//电文条数
				long num=factory.totallnum.incrementAndGet();
				logger.info("接收时间:{},电文号:{}长度:{},报文内容:{},第{}条报文",formatime.format(nowDate), vo.getMsgKey(),lengths, hexString,num);
				//分解电文内容

                try{
                    String date=hexString.substring(0,12).trim().replace("-","/");
                    String system=hexString.substring(12,24).trim();
                    String monitor=hexString.substring(24,36).trim();
                    String item=hexString.substring(36,48).trim();
                    String value=hexString.substring(48,60).trim();
                    String testcode=system+monitor+item;
                    //写入检化验数据的数据
                    oracle.queryInsertData(testcode);
                    oracle.upData(testcode,value,date);
                    oracle.insertnum(vo.getMsgKey(),date,num);//写入历史电文数
                }catch (Exception e){
                    logger.error("检查检化验报文内容是否正确");
                }

			}else if(vo.getMsgKey().equals(factory.getYieldkey())){
				//计划产量数据的数据
				long yieldnum=factory.yieldtotallnum.incrementAndGet();
				logger.info("接收时间:{},电文号:{},长度:{},报文内容:{},第{}条报文",formatime.format(nowDate), vo.getMsgKey(),lengths, hexString,yieldnum);
		        try{
                    String date=hexString.substring(0,10).trim().replace("-","/");
                    String index_id=hexString.substring(10,16).trim();
                    String index_value=hexString.substring(16,34).trim();
                    double value= Double.parseDouble(index_value);
                    String index=index_id.substring(5,6);
                    if(index.equals("2")||index.equals("4")||index.equals("5")||index.equals("6")){
                    	//产量代码的最后一位2为昨日计划，4为月累计计划，5为今日计划，6为月计划
                        //写入月计划和日计划
                        oracle.insertPlanYield(index_id,value,date);
                        oracle.insertnum(vo.getMsgKey(),date,yieldnum);//写入电文条数
                    }else if(index.equals("0")||index.equals("1")){
                    	//写入月实绩和日实绩
                        oracle.insertComperYield(index_id,value,date);
                        oracle.insertnum(vo.getMsgKey(),date,yieldnum);//写入电文条数
                    }else {
                        return;
                    }

                }catch (Exception e){
		            logger.info("检查产量电文内容是否正确");
                }
			}else if(vo.getMsgKey().equals(factory.getWaterkey())){
				//球团新水
				long waternum=factory.watertotallnum.incrementAndGet();
				logger.info("接收时间:{},电文号:{},长度:{},报文内容:{},第{}条报文",formatime.format(nowDate), vo.getMsgKey(),lengths, hexString,waternum);
				try{
					String date=hexString.substring(0,12).trim().replace("-","/");
					String index_value=hexString.substring(12,24).trim();
					//String index_value=hexString.substring(24,36).trim();
					double value= Double.parseDouble(index_value)/1000;
					oracle.insertWater(value,date);
					oracle.insertnum(vo.getMsgKey(),date,waternum);//写入电文条数
                    double valueAvg=value/24;
                    //----------------------yan
                    String dateHourMinuteSec=date+" 00:00:00";
                    List<String>  dateResult = getDateList(dateHourMinuteSec);
                    for (String strDate:dateResult) {
                        oracle.insertWaterHour( valueAvg, strDate);
                    }
                    //----------------------


				}catch (Exception e){
					logger.info("请检查新水的电文内容是否正确");
				}

			}else  {
				logger.info("没有可以匹配的电文号");
			}
			oracle.disconnection();
		}
	}
	//心跳电文答复，写入并推送到通道
//	private void responseHeartbeat(PlcVo vo){
//		PlcVo responsePlcvo= new PlcVo();
//		responsePlcvo.setMsgKey(factory.getHeartbeatkey());
//		getChannel().writeAndFlush(responsePlcvo);
//	}
	@Override
	public String[] getDeviceIds() {
		return new String[0];

	}
	@Override
	public void onOpen() {
		super.onOpen();
		logger.info("建立连接，channel:{}", getChannel());
	}
	
	@Override
	public void onClose() {
		super.onClose();
		factory = null;
	}

	public PlcIoSession(PlcSessionFactory factory) {
		super();
		this.factory = factory;
	}
	public void close() {
		if(getChannel() != null) { 
			try {
				getChannel().close().sync();
			} catch (InterruptedException e) {
				logger.warn(e.getLocalizedMessage(), e);
			} 
		}

	}


	public String gwPrefix() {
		if(gwPrefixCache != null) {
			return gwPrefixCache;
		}
		gwPrefixCache = "";
		if(socketGateway != null && socketGateway.getInstanceVo() != null) {
			gwPrefixCache = "网关Id:" + socketGateway.getInstanceVo().getRunId() + "连接:" + getChannel() + " ";
		}else {
			gwPrefixCache = "连接:" + getChannel() + " ";
		}

		return gwPrefixCache;
	}

	public long getLastAliveTime()
	{
		return lastAliveTime;
	}

    /**
     *
     * @param dateParm 天整点时间
     * @return 前一天21:00到天的20:00的时间string数组
     */
	public List<String> getDateList(String dateParm){
        List<String> resultData=new ArrayList<>();
        Calendar calendar =new GregorianCalendar();
        java.util.Date date = null;
        try {
            date = formatime.parse(dateParm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        calendar.setTime(date);
        //时间减去3个小时。
        calendar.add(calendar.HOUR_OF_DAY, -4);
        Date startDate = calendar.getTime();
        for (int i=0;i<24;i++){
            String dateString = formatime.format(startDate);
            resultData.add(dateString);//将需要返回的时间加入返回数组
            Calendar calendar1 = new GregorianCalendar();
            calendar1.setTime(startDate);
            calendar1.add(calendar.HOUR_OF_DAY, 1);
            startDate = calendar1.getTime();
        }
        return resultData;
    }


}