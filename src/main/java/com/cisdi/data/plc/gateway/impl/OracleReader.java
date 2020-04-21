package com.cisdi.data.plc.gateway.impl;
import oracle.sql.DATE;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class OracleReader {
    /**
     * JDBC的连接对象实例
     */
    private Connection conn = null;//连接的对象
    /**
     *连接到Oracle的驱动名称，从OPCClient类传入
     */
    private String driver; //驱动
    /**
     *连接到Oracle的url串，从OPCClient类传入
     */
    private String url; //连接字符串
    /**
     *连接到Oracle的用户名，从OPCClient类传入
     */
    private String username; //用户名
    /**
     *连接到Oracle的密码，从OPCClient类传入
     */
    private String password; //密码
    /**
     *向Oracle发送语句的语句对象，通常使用问号（?）来表示参数，等后续再填入问号（?）中的参数
     */
    private PreparedStatement pre = null;// 创建预编译语句对象，一般都是用这个而不用Statement
    /**
     *向Oracle数据库执行语句后返回的结果对象
     */
    private ResultSet result = null;// 创建一个结果集对象

    /**
     *当前类中使用的日志类实例
     */
    private static org.slf4j.Logger log = LoggerFactory.getLogger(PlcIoSession.class);
    /**
     *数据库重连次数，这里默认设置为6次
     */
    private static int num=6;

    /**
     * 定义日期格式
     */
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat formatime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


    /**
     * 构造函数，该函数为大部分成员变量进行了赋值
     * @param user Oracle数据库的用户名
     * @param pwd Oracle数据库的密码
     * @param drvier Oracle数据库的驱动程序名
     * @param URL Oracle数据库的连接URL
     */
    OracleReader(String user, String pwd, String drvier, String URL)
    {
        this.driver=drvier;
        this.url=URL;
        this.username=user;
        this.password=pwd;
    }
    /**
     *连接方法，该方法连接到数据库，它将会被递归调用直至连接成功或者超出最大连接次数private static int num
     */

    public void connection()
    {
        try
        {
            Class.forName(driver);
            conn= DriverManager.getConnection(url,username,password);
            log.info("【DB】建立与数据库(URL:"+url+")的连接");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("【DB】SQL:连接失败，等待重连，剩余重连次数"+--num+" 异常信息："+e.getLocalizedMessage());
            if(num<=0)
            {
                log.info("【DB】SQL:重连失败，进程退出");
                System.exit(-1);
            }
            try
            {
                Thread.sleep(100000);
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
                log.info("【DB】SQL:等待失败，进程退出");
            }
            this.connection();
        }
    }

    //更新s_monitor_date的数据，如果有test_code就先把数据更新，没有就把test_code插入到s_monitor_date数据表里
    public void upData(String code,String value,String time)
    {

        ParsePosition pos = new ParsePosition(0);

        String sql="begin update s_monitor_data t set t.apport_value=?,t.clock=? where t.test_code=? ; " +
                " if sql%notfound then  " +
                " insert into s_monitor_data(test_code,apport_value,clock) values(?,?,?); end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,Double.parseDouble(value));
            pre.setDate(2, new java.sql.Date(sdf.parse(time).getTime()));
            pre.setString(3,code);
            pre.setString(4,code);
            pre.setDouble(5,Double.parseDouble(value));
            pre.setDate(6, new java.sql.Date(sdf.parse(time).getTime()));
            pre.execute();
            log.info("sql更新或者添加语句执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }
    //先查询s_monitor_data中的testcode,如果没有testcode就写入s_monitor_data,否则就把当前testcode值写入到S_MONITOR_HISTORYDATA
    public void queryInsertData(String code)
    {
        String sql="insert into S_MONITOR_HISTORYDATA (start_clock,apport_value,unit,test_code)" +
                " (select clock,apport_value,unit,test_code from s_monitor_data where test_code=?)";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setString(1,code);
            pre.execute();
            log.info("sql查询并写入数据语句执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }


//插入报文条数
    public void insertnum(String msgkey,String date,long num)
    {

        String sql="begin update s_sockettest t set t.num=? where t.msgkey=? and t.rev_date=?;  " +
                "   if sql%notfound then  " +
                "  insert into s_sockettest (msgkey,rev_date,num) values(?,?,?); end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setLong(1,num);
            pre.setString(2,msgkey);
            pre.setDate(3, new java.sql.Date(sdf.parse(date).getTime()));
            pre.setString(4,msgkey);
            pre.setDate(5, new java.sql.Date(sdf.parse(date).getTime()));
            pre.setLong(6,num);
            pre.execute();
            log.info("sql写入电文历史条数数据语句执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }

    public void insertWater(double index_value,String date)
    {

//        String sql="insert into S_CALCRESULTDATA (datetype,resultdate,calcpointid,calcpointvalue)values " +
//                "((SELECT UD4 FROM T_CALCPOINT WHERE POINTID=?), " +
//                "?,(SELECT UD1 from T_CALCPOINT WHERE POINTID=?), " +
//                "?)";
        String sql="begin update S_CALCRESULTDATA t " +
                "                set t.calcpointvalue=? where t.datetype=3 and " +
                "                t.resultdate=? and " +
                "                t.calcpointid='QTXS'; " +
                "                if sql%notfound then " +
                "                insert into S_CALCRESULTDATA (datetype,resultdate,calcpointid,calcpointvalue) values " +
                "                (3,?,'QTXS',?); " +
                "                 end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,index_value);
            pre.setDate(2, new java.sql.Date(sdf.parse(date).getTime()));
            pre.setDate(3, new java.sql.Date(sdf.parse(date).getTime()));
            pre.setDouble(4,index_value);
            pre.execute();
            log.info("sql写入水量执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }

    public void insertWaterHour(double index_value,String date)
    {
//
//        String sql="insert into S_CALCRESULTDATA (datetype,resultdate,calcpointid,calcpointvalue)values " +
//                "((SELECT UD2 FROM T_CALCPOINT WHERE POINTID=?), " +
//                "?,(SELECT UD1 from T_CALCPOINT WHERE POINTID=?), " +
//                "?)";
        String sql="begin update S_CALCRESULTDATA t " +
                "                set t.calcpointvalue=? where t.datetype=1 and " +
                "                t.resultdate=? and " +
                "                t.calcpointid='QTXS'; " +
                "                if sql%notfound then " +
                "                insert into S_CALCRESULTDATA (datetype,resultdate,calcpointid,calcpointvalue) values " +
                "                (1,?,'QTXS',?); " +
                "                 end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,index_value);
            pre.setDate(2, new java.sql.Date(formatime.parse(date).getTime()));
            pre.setDate(3, new java.sql.Date(formatime.parse(date).getTime()));
            pre.setDouble(4,index_value);
            pre.execute();
            log.info("sql写入球团每小时水量执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }

    //插入计划数据
    public void insertPlanYield(String index_id,double value,String rectime)
    {
        //写入产量3个主键，planname,rectime,datetype。
//        String sql="insert into s_xcomplandata(planname,rectime,value,datetype)values " +
//                " ((select DISTINCT t.jkname from t_xcomplan t where t.index_id=?),?, " +
//                " ?,(select DISTINCT t.datetype from t_xcomplan t where t.index_id=?))";

        //查找计划产量3个主键，planname,rectime,datetype,如果存在这3个联合主键就更新值value，如果不存在插入值。
        String sql="begin update s_xcomplandata t " +
                " set t.value=? where t.PLANNAME=(select DISTINCT t.jkname from t_xcomplan t where t.index_id=?) " +
                " and t.DATETYPE=(select DISTINCT t.datetype from t_xcomplan t where t.index_id=?) " +
                " and t.RECTIME=?;  " +
                " if sql%notfound then " +
                "insert into s_xcomplandata(planname,rectime,value,datetype)values " +
                " ((select DISTINCT t.jkname from t_xcomplan t where t.index_id=?),? , " +
                "   ?,(select DISTINCT t.datetype from t_xcomplan t where t.index_id=? )); " +
                "end if;end;";

        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,value);
            pre.setString(2,index_id);
            pre.setString(3,index_id);
            String indexLastChar=index_id.substring(5,6);
            if(indexLastChar.equals("5")){
                //如果为5为今天计划，日期加1
                String newDate=addOneDay(rectime);//日期加一天
                pre.setDate(4,new java.sql.Date(sdf.parse(newDate).getTime()));
                pre.setDate(6,new java.sql.Date(sdf.parse(newDate).getTime()));

            }else {
                pre.setDate(4, new java.sql.Date(sdf.parse(rectime).getTime()));
                pre.setDate(6, new java.sql.Date(sdf.parse(rectime).getTime()));
            }
            pre.setString(5,index_id);
            pre.setDouble(7,value);
            pre.setString(8,index_id);

            pre.execute();
            log.info("sql写入实绩产量语句执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }
    //插入实际产量数据
    public void insertComperYield(String index_id,double value,String rectime)
    {

//        String sql="insert into s_xcomperfordata(planname,rectime,value,datetype)values " +
//                " ((select DISTINCT t.jkname from t_xcomperform t where t.index_id=?),?, " +
//                " ?,(select DISTINCT t.datetype from t_xcomperform t where t.index_id=?))";

        //查找实绩产量3个主键，planname,rectime,datetype,如果存在这3个联合主键就更新值value，如果不存在插入值。
        String sql="  begin update s_xcomperfordata t " +
                "    set t.value=? where " +
                "   t.PLANNAME=(select DISTINCT t.jkname from t_xcomperform t where t.index_id=?) " +
                "    and t.DATETYPE=(select DISTINCT t.datetype from t_xcomperform t where t.index_id=?) " +
                "    and t.RECTIME=? ; " +
                "    if sql%notfound then " +
                "    insert into s_xcomperfordata(planname,rectime,value,datetype)values  " +
                "       ((select DISTINCT t.jkname from t_xcomperform t where t.index_id=?),?,  " +
                "       ?,(select DISTINCT t.datetype from t_xcomperform t where t.index_id=?)); " +
                "  end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,value);
            pre.setString(2,index_id);
            pre.setString(3,index_id);
            pre.setDate(4, new java.sql.Date(sdf.parse(rectime).getTime()));
            pre.setString(5,index_id);
            pre.setDate(6, new java.sql.Date(sdf.parse(rectime).getTime()));
            pre.setDouble(7,value);
            pre.setString(8,index_id);
            pre.execute();
            log.info("sql写入实际产量语句执行成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }

    //修改为s_tele_date表,插入方法
    public void insetTeleData(String index_id,double value,String rectime)
    {
        String sql=" begin update S_TELE_DATA t set t.value=? where t.TELE_ID=? and " +
                "t.START_TIME=? ; " +
                " if sql%notfound then  " +
                "insert into " +
                "S_TELE_DATA(TELE_ID,VALUE,START_TIME) values " +
                "(?,?,?); " +
                " end if;end;";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setDouble(1,value);
            pre.setString(2,index_id);
            pre.setDate(3, new java.sql.Date(sdf.parse(rectime).getTime()));
            pre.setString(4,index_id);
            pre.setDouble(5,value);
            pre.setDate(6, new java.sql.Date(sdf.parse(rectime).getTime()));
            pre.execute();
            log.info("sql:"+sql+"写入数据成功");
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句插入数据执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return ;
    }
    //查询每个电文标识tele_id对应的开始时间最新的那条，并返回开始时间最新的那条。
    public  void  UpdateEndTIME(String index_id,String rectime){

//        String sql=" update S_TELE_DATA t set t.END_TIME=?  where t.TELE_ID=?and " +
//                " t.START_TIME= (select START_TIME from (select START_TIME from S_TELE_DATA where TELE_ID=? ORDER BY START_TIME desc) where ROWNUM=1) ";


        String sql=" update S_TELE_DATA t set t.END_TIME=?  where t.TELE_ID=?and " +
                " t.START_TIME=?";
        try
        {   java.util.Date startTime=sdf.parse(getNewStartTime(index_id));
            java.util.Date endTme=sdf.parse(rectime);
            if(endTme.after(startTime)){
                pre=conn.prepareStatement(sql);
                pre.setDate(1, new java.sql.Date(sdf.parse(rectime).getTime()));
                pre.setString(2,index_id);
                pre.setDate(3,new java.sql.Date(startTime.getTime()));
                pre.executeQuery();
            }
        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句更新结束时间执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }
        return;
    }


    public String getNewStartTime(String index_id)
    {
        ResultSet myResult=null;
        String starTime=null;

        String sqlStarTime="select START_TIME from (select START_TIME from S_TELE_DATA where TELE_ID=? ORDER BY START_TIME desc) where ROWNUM=1";
        try
        {

            pre=conn.prepareStatement(sqlStarTime);
            pre.setString(1,index_id);
            myResult=pre.executeQuery();
            myResult.next();
            starTime= sdf.format(myResult.getDate("START_TIME"));

        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sqlStarTime+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return starTime;
    }


    public int getNum(String index_id)
    {
        ResultSet myResult=null;
        Integer nums=null;

        String sql=" select count(*) from S_TELE_DATA where tele_id=?";
        try
        {
            pre=conn.prepareStatement(sql);
            pre.setString(1,index_id);
            myResult=pre.executeQuery();
            myResult.next();
            nums= myResult.getInt("count(*)");

        }
        catch (Exception e)
        {
            log.error("【DB】SQL语句"+sql+"执行失败，数据表是否更改:"+e.getLocalizedMessage());
        }

        return nums;
    }


    public String addOneDay(String rectime){
        Calendar calendar =new GregorianCalendar();
        java.util.Date date = null;
        try {
            date = sdf.parse(rectime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        calendar.setTime(date);
        //今天计划在原有的基础上加上一天。
        calendar.add(calendar.DATE, 1);
        java.util.Date utilDate ;
        utilDate = calendar.getTime();
        String dateString = sdf.format(utilDate);
        return  dateString;
    }

//    public ArrayList<Pair<String,String>> getAllGroup()
//    {
//        ArrayList<Pair<String,String>> rtList=new ArrayList<>();
//
//        String sql="select scanrate,unit from tag_index_test group by scanrate,unit";
//        ResultSet myResult=null;
//        try
//        {
//            pre=conn.prepareStatement(sql);
//            myResult=pre.executeQuery();
//            while(myResult.next())
//            {
//                rtList.add(new Pair<String,String>(myResult.getString("scanrate"),myResult.getString("unit")));
//            }
//        }
//        catch (Exception e)
//        {
//            log.error("【DB】SQL语句"+sql+"执行失败，请检查配置文件是否正确，以及数据表是否更改:"+e.getLocalizedMessage());
//        }
//
//        return rtList;
//    }

    /**
     * 断开连接的方法
     */
    public void disconnection()
    {
        try
        {
            if (result != null)
            {
                result.close();
            }
            if (pre != null)
            {
                pre.close();
            }
            if (conn != null)
            {
                conn.close();
            }
        }
        catch (Exception e)
        {

            e.printStackTrace();
            log.error("【DB】断开连接时异常："+e.getLocalizedMessage());
        }

        log.info("【DB】断开与Oracle数据库(URL:"+url+")的连接");
    }
}
