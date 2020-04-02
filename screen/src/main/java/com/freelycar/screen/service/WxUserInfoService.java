package com.freelycar.screen.service;

import com.freelycar.screen.entity.WxUserInfo;
import com.freelycar.screen.entity.repo.WxUserInfoRepository;
import com.freelycar.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @author pyt
 * @date 2020/4/1 15:38
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class WxUserInfoService {
    @Autowired
    private WxUserInfoRepository wxUserInfoRepository;

    /**
     * 最近12月每月新增注册用户数
     *
     * @return List
     */
    public List<Map<String, Integer>> getMonthlyAdditions() {
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<WxUserInfo> wxUserInfoList = wxUserInfoRepository.findByDelStatusAndCreateTimeAfter(false, start);
        return MonthService.getMonthlyAdditions(wxUserInfoList, now);
    }
}
