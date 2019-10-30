package com.cisdi.data.plc.gateway.impl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.cisdi.data.sdk.gateway.netty.IoSession;
import com.cisdi.data.sdk.gateway.netty.impl.AbstractSessionFactory;


public class PlcSessionFactory extends AbstractSessionFactory {
  public final static AtomicLong totallnum = new AtomicLong();
  public final static AtomicLong yieldtotallnum=new AtomicLong();
  public final static AtomicLong watertotallnum=new AtomicLong();


    private String user;
    private String pwd;
    private String driver;
    private String url;
    private String sendDC;
    private String heartbeatkey;
    private String jhykey;
    private String yieldkey;
    private String waterkey;

    public   PlcSessionFactory(){
        super();
    }

    public   PlcSessionFactory(String user,String pwd,String driver,String url,String sendDC,String heartbeatkey,String jhykey,String yieldkey,String waterkey){
        super();
        this.user=user;
        this.pwd=pwd;
        this.driver=driver;
        this.url=url;
        this.heartbeatkey=heartbeatkey;
        this.jhykey=jhykey;
        this.sendDC=sendDC;
        this.yieldkey=yieldkey;
        this.waterkey=waterkey;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSendDC() {
        return sendDC;
    }

    public void setSendDC(String sendDC) {
        this.sendDC = sendDC;
    }

    public String getHeartbeatkey() {
        return heartbeatkey;
    }

    public void setHeartbeatkey(String heartbeatkey) {
        this.heartbeatkey = heartbeatkey;
    }

    public String getJhykey() {
        return jhykey;
    }

    public void setJhykey(String jhykey) {
        this.jhykey = jhykey;
    }

    public String getYieldkey() {
        return yieldkey;
    }

    public void setYieldkey(String yieldkey) {
        this.yieldkey = yieldkey;
    }

    public String getWaterkey() {
        return waterkey;
    }

    public void setWaterkey(String waterkey) {
        this.waterkey = waterkey;
    }

    @Override
	public IoSession newSession() {
		IoSession session = new PlcIoSession(this);
		session.init(UUID.randomUUID().toString(), provider, socketGateway);
		sessionSet.add(session);
		return session;
	}
}
