package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.project.entity.WxCumulate;
import com.freelycar.saas.project.repository.WxCumulateRepository;
import com.freelycar.saas.util.TimestampUtil;
import com.freelycar.saas.util.UpdateTool;
import com.freelycar.saas.wxutils.WechatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;

@Service
@Transactional(rollbackFor = Exception.class)
public class WxCumulateService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WxCumulateRepository wxCumulateRepository;

    /**
     * 新增/修改
     *
     * @param wxCumulate
     * @return
     * @throws ArgumentMissingException
     */
    WxCumulate saveOrUpdate(WxCumulate wxCumulate) throws ArgumentMissingException {
        if (null == wxCumulate) {
            throw new ArgumentMissingException();
        }
        Long id = wxCumulate.getId();
        if (StringUtils.isEmpty(id)) {
            wxCumulate.setCreateTime(TimestampUtil.getCurrentTimestamp());
        } else {
            WxCumulate source = this.findWxCumulateById(id);
            UpdateTool.copyNullProperties(source, wxCumulate);
        }
        return wxCumulateRepository.saveAndFlush(wxCumulate);
    }


    @Scheduled(cron = "0 10 8 * * ?")
    void saveWxCumulateTask() {
        String yesterdayDate = TimestampUtil.getYesterday();
        try {
            JSONObject jsonObject = WechatConfig.getUserCumulate(yesterdayDate, yesterdayDate);
            JSONArray wxCumulateObj = jsonObject.getJSONArray("list");

            String refDate = wxCumulateObj.getJSONObject(0).getString("ref_date");
            Long cumulateUser = wxCumulateObj.getJSONObject(0).getLong("cumulate_user");

            if (StringUtils.hasText(refDate) && null != cumulateUser) {
                WxCumulate wxCumulate = new WxCumulate();
                wxCumulate.setRefDate(refDate);
                wxCumulate.setCumulateUser(cumulateUser);
                WxCumulate res = this.saveOrUpdate(wxCumulate);
                logger.info("定时任务saveWxCumulateTask执行成功，存入数据库的数据为：");
                logger.info(res.toString());
            } else {
                logger.error("定时任务saveWxCumulateTask中，微信返回数据中为找到有效的refDate和cumulateUser");
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            logger.error("定时任务saveWxCumulateTask执行异常");
        }
    }

    /**
     * 根据日期查找关注数对象
     *
     * @param refDate
     * @return
     * @throws ArgumentMissingException
     */
    public WxCumulate findWxCumulateByDate(String refDate) throws ArgumentMissingException {
        if (StringUtils.isEmpty(refDate)) {
            throw new ArgumentMissingException();
        }
        return wxCumulateRepository.findTopByRefDateOrderByCreateTimeDesc(refDate);
    }

    /**
     * 根据id查找关注数对象
     *
     * @param id
     * @return
     */
    public WxCumulate findWxCumulateById(Long id) {
        return wxCumulateRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    /**
     * 根据日期返回关注数
     *
     * @param refDate
     * @return
     * @throws ArgumentMissingException
     */
    public Long cumulateUserByDate(String refDate) throws ArgumentMissingException {
        WxCumulate wxCumulate = this.findWxCumulateByDate(refDate);
        if (null == wxCumulate) {
            return null;
        }
        return wxCumulate.getCumulateUser();
    }
}
