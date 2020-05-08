package com.cisdi.data.plc.gateway.impl;
import com.cisdi.data.common.exception.BusinessException;
import com.cisdi.data.sdk.gateway.base.SocketGatewayBase;
import com.cisdi.data.sdk.procotol.message.SocketReturnMessage;
import com.cisdi.data.sdk.service.ServiceProvider;
import com.cisdi.data.sdk.vo.ExeResultVo;
import com.google.common.primitives.Bytes;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cisdi.data.sdk.consts.ServiceName;
import com.cisdi.data.sdk.enums.ByteOrder;
import com.cisdi.data.sdk.gateway.message.SocketMessage;
import com.cisdi.data.sdk.gateway.netty.IoSession;
import com.cisdi.data.sdk.gateway.netty.impl.AbstractIoSession;
import com.cisdi.data.sdk.service.SendService;
import io.netty.buffer.ByteBuf;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlcIoSession extends AbstractIoSession implements IoSession {
	private PlcSessionFactory factory ;
    private final byte StringAppend = (byte)' ';
    private final byte LongPrefix = (byte)'0';
	private final Charset charset = Charset.forName("GBK");
    Date dateTimeNow=new Date();
	OracleReader oracle=null;
    Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
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
        /**
         * 发送应答电文
         */
        sendRespose(response(vo));
        if(vo != null && serviceProvider != null && socketGateway != null) {
        	SendService service = (SendService)serviceProvider.getByName(ServiceName.Send);
    		SocketMessage socketMessage = socketGateway.buildSocketMessage();
    		socketMessage.setDeviceId(vo.getSendDC()+"");
    		//socketMessage.setData(vo.getBody());
    		socketMessage.setMsgKey(vo.getMsgKey());
    		service.sendMessage(socketMessage);
        }else {
        	//电文长度
			int lengths=0;
            Date nowDate=new Date();
            lengths=vo.getBody().length();
            /**
             * 收到心跳电文，不做处理
             */
            if(lengths==0){
                logger.info("心跳电文,接收时间:{},电文号:{}",formatime.format(nowDate), vo.getMsgKey());
                return;
            }
			//电文数据体
			String hexString=vo.getBody();

			 //查询数据，写入数据。
			if(vo.getMsgKey().equals(factory.getJhykey())){
				//输出电文信息，电文长度，内容，条数
				//电文条数
				long num=factory.totallnum.incrementAndGet();
				logger.info("接收时间:{},电文号:{}长度:{},报文内容:{},第{}条报文",formatime.format(nowDate), vo.getMsgKey(),lengths, hexString,num);
				//分解电文内容

                try{
                    //接收检化验数据date,system,monitor,item,value。
                    String date=hexString.substring(0,12).trim().replace("-","/");
                    String system=hexString.substring(12,24).trim();
                    String monitor=hexString.substring(24,36).trim();
                    String item=hexString.substring(36,48).trim();
                    String testCodeValue=hexString.substring(48,60).trim();
                    //testcode(system+monitor+item)为数据表里的唯一标识
                    String testcode=system+monitor+item;
                    //判断value是否为空或者非数字
                    Matcher isNum = pattern.matcher(testCodeValue);
                    if(testCodeValue.equals(null)||!isNum.matches()){
                        logger.warn("检化验testcode："+testcode+"value值为："+testCodeValue+"包含有不非数字字符，或者为null");
                        return;
                    }
                    double value= Double.parseDouble(testCodeValue);
                    //写入到S_MONITOR_HISTORYDATA,S_MONITOR_DATA检化验数据的数据,为检化验数据为数据表和历史表
                    oracle.queryInsertData(testcode);
                    oracle.upData(testcode,testCodeValue,date);

//                    //写入到s_tele_data数据表
//                    //数据表里存在当前testcod
//                    if( oracle.getNum(testcode)!=0){
//                        //先需要找到当前testcode对应的开始时间最新一条数据,将date写入到结束时间end_time,完成数据的有效周期。
//                        oracle.UpdateEndTIME(testcode,date);
//                    }
//                    //数据表里面没有记录过这个testcode，则直接写入数据。
//                    oracle.insetTeleData(testcode,value,date);
//                    //记录插入电文的数量
//                    oracle.insertnum(vo.getMsgKey(),date,num);
                    insertDataToTeleData(testcode,date,value,num,vo.getMsgKey());
                }catch (Exception e){
                    logger.error("检查检化验报文内容是否正确",e.getLocalizedMessage());
                }

			}else if(vo.getMsgKey().equals(factory.getYieldkey())){
				//计划产量数据的数据
				long yieldnum=factory.yieldtotallnum.incrementAndGet();
				logger.info("接收时间:{},电文号:{},长度:{},报文内容:{},第{}条报文",formatime.format(nowDate), vo.getMsgKey(),lengths, hexString,yieldnum);
		        try{
                    String date=hexString.substring(0,10).trim().replace("-","/");
                    String index_id=hexString.substring(10,16).trim();
                    String index_value=hexString.substring(16,34).trim();
                    Matcher isNum = pattern.matcher(index_value);
                    if(index_value.equals(null)||!isNum.matches()){
                        logger.warn("产量电文："+index_id+"value值为："+index_value+"包含有不非数字字符，或者为null");
                        return;
                    }
                    double value= Double.parseDouble(index_value);
                    String index=index_id.substring(5,6);

                    // 写入到分别的实绩产量和计划产量表中。
                    if(index.equals("2")||index.equals("4")||index.equals("5")||index.equals("6")){
                    	//产量代码的最后一位2为昨日计划，4为月累计计划，5为今日计划，6为月计划
                        //写入月计划和日计划
                        oracle.insertPlanYield(index_id,value,date);
                       // oracle.insertnum(vo.getMsgKey(),date,yieldnum);//写入电文条数
                    }else if(index.equals("0")||index.equals("1")){
                    	//写入月实绩和日实绩
                        oracle.insertComperYield(index_id,value,date);
                        //oracle.insertnum(vo.getMsgKey(),date,yieldnum);//写入电文条数
                    }else {
                        return;
                    }

                    //写入到s_tele_date表中的数据
                    if(index.equals("2")||index.equals("4")||index.equals("5")||index.equals("6")||index.equals("1")||index.equals("0")){
//                        if( oracle.getNum(index_id)!=0){
//                            //如果num不为0，先需要找到当前index_id对应的开始时间最新一条数据,追加date写入到结束时间。
//                            oracle.UpdateEndTIME(index_id,date);
//                        }
//                             //如果num为0，代表数据表里面没有记录过这个index_id。则直接写入数据。
//                            oracle.insetTeleData(index_id,value,date);
//                            //记录插入的条数
//                            oracle.insertnum(vo.getMsgKey(),date,yieldnum);
                        insertDataToTeleData(index_id,date,value,yieldnum,vo.getMsgKey());
                    }else {
                        return;
                    }
                }catch (Exception e){
		            logger.info("检查计划和实绩产量电文内容是否正确");
                }
			}else if(vo.getMsgKey().equals(factory.getWaterkey())) {
                //球团新水
                long waternum = factory.watertotallnum.incrementAndGet();
                logger.info("接收时间:{},电文号:{},长度:{},报文内容:{},第{}条报文", formatime.format(nowDate), vo.getMsgKey(), lengths, hexString, waternum);
                try {
                    String index_id="QTXS";
                    String date = hexString.substring(0, 12).trim().replace("-", "/");
                    String index_value = hexString.substring(12, 24).trim();
                    double value = Double.parseDouble(index_value);
                    //插入s_calcresultdata数据表
                    oracle.insertWater(value, date);
                    //oracle.insertnum(vo.getMsgKey(), date, waternum);//写入电文条数
                    double valueAvg = value / 24;
                    String dateHourMinuteSec = date + " 00:00:00";
                    List<String> dateResult = getDateList(dateHourMinuteSec);
                    for (String strDate : dateResult) {
                        oracle.insertWaterHour(valueAvg, strDate);
                    }

//                    //写入到s_tele_data数据表里
//                    if( oracle.getNum(index_id)!=0){
//                        //如果num不为0，先需要找到当前index_id对应的开始时间最新一条数据,将date写入到结束时间。
//                        oracle.UpdateEndTIME(index_id,date);
//                    }
//                    //如果num为0，代表数据表里面没有记录过这个index_id。则直接写入数据。
//                    oracle.insetTeleData(index_id,value,date);
//                    //记录插入的条数
//                    oracle.insertnum(vo.getMsgKey(),date,waternum);
                    insertDataToTeleData(index_id,date,value,waternum,vo.getMsgKey());

                } catch (Exception e) {
                    logger.info("请检查新水的电文内容是否正确");
                }

			}else  {

			      logger.info("没有与"+vo.getMsgKey()+"可以匹配的电文号，长度:{},报文内容:{}", lengths, hexString);

			}
            /**
             * 断开数据库和连接通道
             */
            close();

		}
	}

	//接收到电文写入到s_tele_data数据表中，记录电文条数。根据时间判断插入结束时间
	public void  insertDataToTeleData(String index_id,String date,double value,long teleNum,String teleKey){
        //写入到s_tele_data数据表里
        if( oracle.getNum(index_id)!=0){
            //如果num不为0，先需要找到当前index_id对应的开始时间最新一条数据,将date写入到结束时间。
            oracle.UpdateEndTIME(index_id,date);
        }
        //如果num为0，代表数据表里面没有记录过这个index_id。则直接写入数据。
        oracle.insetTeleData(index_id,value,date);
        //记录插入的条数
        oracle.insertnum(teleKey,date,teleNum);
    }

	@Override
	public String[] getDeviceIds() {
		return new String[0];

	}


    public void sendRespose(byte[] message){

        ByteBuf buf = Unpooled.wrappedBuffer(message);
        if(null == channel) {
            throw new BusinessException("channel 为 null");
        }

        if(message == null) {
            throw new BusinessException("message 为 null");
        }

        try {
            channel.writeAndFlush(buf).sync();

        } catch (InterruptedException e) {
            logger.warn("发送消息失败:{}", e);
        }


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
       // logger.info("断开连接onclose，channel:{}", getChannel());
	}

	public PlcIoSession(PlcSessionFactory factory) {
		super();
		this.factory = factory;
	}
	public void close() {
		if(getChannel() != null) { 
			try {

			    logger.info("断开连接，channel:{}", getChannel());
				getChannel().close().sync();
                /**
                 * 数据库关闭
                 */
                oracle.disconnection();

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
    //电文答复，写入并推送到通道，发送
    private  byte[]  response(PlcVo vo){
	    String message=null;
        byte[] concated=null;
        try{
            String headApartString=vo.getMsgKey()+vo.getDate()+vo.getTime()+vo.getSendDC()+vo.getRevDC()+vo.setFun(factory.getFun());
            //电文头长度
            byte[] headlengthByte=PlcVoEncoder.encodeInteger(110,charset,4,LongPrefix);
            //电文其他部分，电文号，日期，时间，发送端，接收端，功能码
            byte[] headApartByte=headApartString.getBytes(charset);
            //控制域c80
            byte[] controlDomainByte=PlcVoEncoder.encodeString(" ",charset,80,StringAppend);
            //结束符
            byte[] endByte = new byte[1];
            endByte[0] =0x0a;
            concated= Bytes.concat(headlengthByte,headApartByte,controlDomainByte,endByte);
            message = new String(concated, CharsetUtil.US_ASCII);
            getChannel().write(concated);
            getChannel().flush();
            logger.info("【应答电文】发送时间:{},电文头:{},电文功能码:{},电文:{}",formatime.format(dateTimeNow), vo.getMsgKey(),vo.getFun(),message);
        }catch (Exception e){
            logger.error("应答电文发送失败,请检查应答电文格式是否正确！",e.getLocalizedMessage(), e);
        }

        return concated;
    }
}