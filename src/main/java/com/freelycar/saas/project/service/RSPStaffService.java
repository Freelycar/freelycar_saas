package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.project.entity.RSPProject;
import com.freelycar.saas.project.entity.Staff;
import com.freelycar.saas.project.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 14:34
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RSPStaffService {
    private StaffRepository staffRepository;

    @Autowired
    public void setStaffRepository(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    /**
     * 添加技师
     */
   /* public Staff add(Staff staff) throws DataIsExistException, ArgumentMissingException {
        if (staff != null && project.getName() != null && !project.getName().equals("")) {
            String name = project.getName();
            Optional<RSPProject> optionalRSPProject = rspProjectRepository.findByName(name);
            if (optionalRSPProject.isPresent()) {
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
    }*/

    /**
     * 删除技师（单个、批量）
     */
    /*@Transactional(rollbackFor = BatchDeleteException.class)
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

    *//**
     * 服务商下技师列表
     *//*
    public Page<Staff> list(String name, Integer currentPage, Integer pageSize) {
        return rspProjectRepository.findByDelStatusAndNameContainingOrderByIdAsc(Constants.DelStatus.NORMAL.isValue(), name, PageableTools.basicPage(currentPage, pageSize));
    }*/
}
