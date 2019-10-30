package com.cisdi.data.plc.gateway.impl;

import org.omg.CORBA.PUBLIC_MEMBER;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @作者: yq
 * @描述: 定时器
 * @日期: 2019-09-10 14:45
 */
public class NFDFlightDataTimerTask extends TimerTask {

    @Override
    public  void run(){

        try{
//            AtomicLong atomicLongjhy=new AtomicLong(0);
//            AtomicLong atomicyield=new AtomicLong(0);

            SimpleDateFormat formatime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date nowDate=new Date();
            PlcSessionFactory.totallnum.set(0);
            PlcSessionFactory.yieldtotallnum.set(0);
            PlcSessionFactory.watertotallnum.set(0);
            System.out.println("定时任务启动后totallnum"+PlcSessionFactory.totallnum+"时间"+formatime.format(nowDate));
            System.out.println("定时任务启动后yieldtotallnum:"+PlcSessionFactory.yieldtotallnum+"时间"+formatime.format(nowDate));
            System.out.println("定时任务启动后watertotallnum:"+PlcSessionFactory.watertotallnum+"时间"+formatime.format(nowDate));
        }catch (Exception e){

            //log.info("信息异常");

        }
    }
}
