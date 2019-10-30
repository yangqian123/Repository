package com.cisdi.data.plc.gateway.impl;



import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

/**
 * @作者: yq
 * @描述: 定时清0
 * @日期: 2019-09-10 13:42
 */
public class TimerManage {
    public static  final long PERIOD_DAY=24*60*60*1000;

    public Date addDay(Date date,int num){
        Calendar startDT=Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH,num);
        return startDT.getTime();
    }
}
