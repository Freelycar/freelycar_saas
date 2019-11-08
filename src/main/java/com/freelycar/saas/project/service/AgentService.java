package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.*;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.Agent;
import com.freelycar.saas.project.repository.AgentRepository;
import com.freelycar.saas.util.UpdateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AgentRepository agentRepository;


    public Agent findById(String id) throws ObjectNotFoundException {
        return agentRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
    }

    public Agent modify(Agent agent) throws ArgumentMissingException, ObjectNotFoundException {
        if (null == agent) {
            throw new ArgumentMissingException();
        }
        String id = agent.getId();
        if (StringUtils.isEmpty(id)) {
            agent.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            agent.setCreateTime(new Timestamp(System.currentTimeMillis()));
        } else {
            Agent source = this.findById(id);
            UpdateTool.copyNullProperties(source, agent);
        }
        return agentRepository.save(agent);
    }

    public PaginationRJO list(Integer currentPage, Integer pageSize, String agentName, String linkMan) {
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        return PaginationRJO.of(agentRepository.findAllByDelStatusAndAgentNameContainingAndLinkManContaining(Constants.DelStatus.NORMAL.isValue(), agentName, linkMan, pageable));
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject delete(String id) {
        try {
            int result = agentRepository.delById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "删除失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "删除失败，删除操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "删除成功");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    public ResultJsonObject delByIds(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return ResultJsonObject.getErrorResult(null, "删除失败：ids" + ResultCode.PARAM_NOT_COMPLETE.message());
        }
        String[] idsList = ids.split(",");
        for (String id : idsList) {
            agentRepository.delById(id);
        }
        return ResultJsonObject.getDefaultResult(null);
    }
}
