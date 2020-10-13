package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.RSPProject;
import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.repository.RSPProjectRepository;
import com.freelycar.saas.util.UpdateTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.*;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-10
 * Time: 15:49
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RSPProjectService {
    private RSPProjectRepository rspProjectRepository;

    @Autowired
    public void setRspProjectRepository(RSPProjectRepository rspProjectRepository) {
        this.rspProjectRepository = rspProjectRepository;
    }

    /**
     * 服务商项目新增/修改
     */
    public RSPProject add(RSPProject project) throws DataIsExistException, ArgumentMissingException, ObjectNotFoundException {
        if (project != null && !StringUtils.isEmpty(project.getId())) {
            String projectId = project.getId();
            //修改
            Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(projectId, Constants.DelStatus.NORMAL.isValue());
            if (rspProjectOptional.isPresent()) {
                RSPProject source = rspProjectOptional.get();
                UpdateTool.copyNullProperties(source, project);
                rspProjectRepository.saveAndFlush(project);
                return project;
            } else throw new ObjectNotFoundException("操作失败，未找到id为：" + projectId + " 的RSPProject对象");
        } else if (project != null
                && !StringUtils.isEmpty(project.getName())
                && !StringUtils.isEmpty(project.getRspId())) {
            //新增
            String name = project.getName();
            List<RSPProject> rspProjectList = rspProjectRepository.findByNameAndRspIdAndDelStatus(name,project.getRspId(),Constants.DelStatus.NORMAL.isValue());
            if (rspProjectList.size() > 0) {
                throw new DataIsExistException("已包含名称为：“" + name + "”的数据，不能重复添加。");
            } else {
                project.setCreateTime(new Timestamp(System.currentTimeMillis()));
                project.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                project = rspProjectRepository.save(project);
                return project;
            }
        } else {
            throw new ArgumentMissingException("参数不完整");
        }
    }

    /**
     * 删除项目（单个、批量）
     */
    @Transactional(rollbackFor = BatchDeleteException.class)
    public ResultJsonObject delete(String[] ids) throws BatchDeleteException {
        Set<String> idSet = new HashSet<>(Arrays.asList(ids));
        int result = rspProjectRepository.delById(idSet);
        if (result == 0) {
            return ResultJsonObject.getErrorResult(ids, "删除失败," + RESULT_DATA_NONE);
        }
        if (result != ids.length) {
            throw new BatchDeleteException("部分id不存在");
        }
        return ResultJsonObject.getDefaultResult(ids, "删除成功");
    }

    /**
     * 服务商下项目列表
     */
    public Page<RSPProject> list(String name, Integer currentPage, Integer pageSize, String rspId) {
        return rspProjectRepository.findByDelStatusAndNameContainingAndRspIdOrderByIdAsc(Constants.DelStatus.NORMAL.isValue(), name, rspId, PageableTools.basicPage(currentPage, pageSize));
    }
}
