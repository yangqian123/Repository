package com.cisdi.data.plc.gateway.impl;

import com.cisdi.data.sdk.gateway.netty.IoSession;
import com.cisdi.data.sdk.gateway.netty.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AliveCheckTask implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(AliveCheckTask.class);

    private SessionFactory sessionFactory;
    private AtomicBoolean shouldRun = null;

    /**
     * 会话允许的保活时间，单位秒
     */
    private int keepAlive;

    private static final double multiply = 1.5;

    private static final int sleepInternal = 1000; // 1秒

    public AliveCheckTask(int keepAlive, AtomicBoolean shouldRun, SessionFactory sessionFactory) {
        super();
        this.keepAlive = keepAlive;
        this.shouldRun = shouldRun;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void run() {
        logger.info("启动plc保活超时检测线程");

        while (shouldRun != null && shouldRun.get() == true) {
            try {
                long now = System.currentTimeMillis();

                List<IoSession> sessions = sessionFactory.getSessions();

                for (IoSession ioSession : sessions) {
                    PlcIoSession session = (PlcIoSession)ioSession;

                    // 超出指定倍数保活时间，
                    if((now - session.getLastAliveTime()) > (multiply * keepAlive * 1000)) {
                        session.close();
                        logger.warn("{} 超出指定倍数{}保活时间{},单位秒，关闭通道",
                                session.gwPrefix(), multiply, keepAlive);
                    }
                }

                Thread.sleep(sleepInternal);
            } catch (Exception e) {
                logger.warn(e.getLocalizedMessage(), e);
            }
        }

        logger.info("结束plc保活超时检测线程");
    }
}
