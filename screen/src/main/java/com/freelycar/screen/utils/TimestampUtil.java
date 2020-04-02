package com.freelycar.screen.utils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pyt
 * @date 2020/4/1 11:11
 * @email 2630451673@qq.com
 * @desc
 */
public class TimestampUtil {
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter dateTimeFormatter1 = DateTimeFormat.forPattern("yyyy-MM");

    /**
     * 从当前时间点往前,共12个月（包含当月）
     * 获取开始时间start(某年某月1号0点)
     * 如：
     * 参数时间为：2020-04-01 14:39:07.426
     * 结果：2019-05-01 00:00:00.000
     * @param end 截止时间
     * @return DateTime
     */
    public static DateTime getStartTime(DateTime end){
        DateTime result = end.minusMonths(11)
                .withDayOfMonth(1)    //当月第一天
                .withHourOfDay(0)     //当天0点
                .withMinuteOfHour(0)  //当小时0分
                .withSecondOfMinute(0)//当分钟0秒
                .withMillisOfSecond(0); //当秒0毫秒
//        System.out.println(result.toString(dateTimeFormatter));
        return result;
    }

    /**
     *  如:
     *  参数时间为：2020-04-01 14:39:07.426
     *  结果为：
     * [2019-05-01 00:00:00.000, 2019-06-01 00:00:00.000, 2019-07-01 00:00:00.000, 2019-08-01 00:00:00.000,
     * 2019-09-01 00:00:00.000, 2019-10-01 00:00:00.000, 2019-11-01 00:00:00.000, 2019-12-01 00:00:00.000,
     * 2020-01-01 00:00:00.000, 2020-02-01 00:00:00.000, 2020-03-01 00:00:00.000, 2020-04-01 00:00:00.000,
     * 2020-04-01 14:39:07.426]
     * @param end
     * @return
     */
    public static List<DateTime> getTimeList(DateTime end){
        DateTime start = getStartTime(end);
        List<DateTime> timeList = new ArrayList<>();
        timeList.add(start);
        for (int i = 1; i < 12; i++) {
            timeList.add(start.plusMonths(i));
        }
        timeList.add(end);
        return timeList;
    }

    public static List<String> getMonthsList(DateTime start){
        List<String> monthsList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            monthsList.add(start.minusMonths(i).toString(dateTimeFormatter1));
        }
        return monthsList;
    }

    public static void main(String[] args) {
//        getStartTime(new DateTime());
//        System.out.println(getMonthsList(new DateTime()));
//        for (DateTime t:
//             getTimeList(new DateTime())) {
//            System.out.println(t.toString(dateTimeFormatter));
//        }
    }

}
