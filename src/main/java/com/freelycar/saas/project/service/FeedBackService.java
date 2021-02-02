package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.basic.wrapper.SortDto;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.FeedBack;
import com.freelycar.saas.project.repository.FeedBackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Timestamp;

/**
 * @Author: pyt
 * @Date: 2021/1/15 10:09
 * @Description:
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FeedBackService {
    @Autowired
    private FeedBackRepository feedBackRepository;

    /**
     * 新建反馈
     */
    public void createFeedBack(FeedBack feedBack) throws ArgumentMissingException {
        String[] feedBackType = {"功能异常", "产品建议", "支付问题", "其他"};
        String feedBackTypes = feedBack.getFeedBackTypes();
        boolean flag = true;
        if (StringUtils.hasText(feedBackTypes)) {
            String[] types = feedBackTypes.split(",");
            for (String type :
                    types) {
                boolean f = false;
                for (int i = 0; i < feedBackType.length; i++) {
                    if (type.equals(feedBackType[i])) {
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    flag = false;
                    break;
                }
            }
        }
        if (flag) {
            String desc = feedBack.getDescription();
            if (null != desc && desc.length() > 100) {
                desc = desc.substring(0, 100);
                feedBack.setDescription(desc);
            }
            feedBack.setCreateTime(new Timestamp(System.currentTimeMillis()));
            feedBackRepository.save(feedBack);
        } else {
            throw new ArgumentMissingException("反馈类型为空或错误");
        }
    }

    /**
     * 反馈列表:
     * 创建时间排序
     * 反馈类型模糊查询
     *
     * @param currentPage
     * @param pageSize
     * @param type
     * @param sort        true-顺序，false-逆序
     * @return
     */
    public Page<FeedBack> list(
            Integer currentPage,
            Integer pageSize,
            String type,
            boolean sort
    ) {
        String orderType = "asc";
        if (!sort) {
            orderType = "desc";
        }
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize, new SortDto(orderType, "createTime"));
        Page<FeedBack> feedBackPage = feedBackRepository.findByFeedBackTypesContaining(type, pageable);
        return feedBackPage;
    }
}
