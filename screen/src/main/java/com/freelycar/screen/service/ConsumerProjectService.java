package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.entity.ConsumerProjectInfo;
import com.freelycar.screen.entity.Project;
import com.freelycar.screen.entity.ProjectType;
import com.freelycar.screen.entity.repo.ConsumerProjectInfoRepository;
import com.freelycar.screen.entity.repo.ProjectRepository;
import com.freelycar.screen.entity.repo.ProjectTypeRepository;
import com.freelycar.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * @author pyt
 * @date 2020/4/2 15:31
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class ConsumerProjectService {
    @Autowired
    private ConsumerProjectInfoRepository consumerProjectInfoRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    /**
     * 服务项目排名
     */
    public JSONObject genProjectRanking() {
        //1.获取项目数据
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<ConsumerProjectInfo> projectInfoList = consumerProjectInfoRepository.findByDelStatusAndCreateTimeAfter(false, start);
        Set<String> projectIds = new HashSet<>();
        List<Map<String, Integer>> projectSumList = new ArrayList<>();
        for (ConsumerProjectInfo project :
                projectInfoList) {
            String projectId = project.getProjectId();
            if (!projectIds.contains(projectId)) {
                projectIds.add(projectId);
                Map<String, Integer> projectSum = new HashMap<>();
                projectSum.put(projectId, 1);
                projectSumList.add(projectSum);
            } else {
                for (Map<String, Integer> projectSum :
                        projectSumList) {
                    for (String key :
                            projectSum.keySet()) {
                        if (key.equals(projectId)) {
                            int sum = projectSum.get(projectId);
                            sum++;
                            projectSum.put(projectId, sum);
                        }
                    }
                }
            }
        }
        //2.获取项目类型
        List<Project> projectList = projectRepository.findByDelStatusAndIdIn(false, projectIds);
        Set<String> projectTypeIds = new HashSet<>();
        List<Map<String, Integer>> projectTypeSumList = new ArrayList<>();
        Map<String, String> projectTypeRelationship = new HashMap<>();
        for (Project project : projectList) {
            String projectTypeId = project.getProjectTypeId();
            if (!projectTypeIds.contains(projectTypeId)) {
                projectTypeIds.add(projectTypeId);
            }
            projectTypeRelationship.put(project.getId(), projectTypeId);
        }
        List<ProjectType> projectTypeList = projectTypeRepository.findAll();
        Map<String, String> projectType = new HashMap<>();
        for (ProjectType type :
                projectTypeList) {
            projectType.put(type.getId(), type.getName());
        }
        Set<String> record = new HashSet<>();
        for (Map<String, Integer> projectSum :
                projectSumList) {
            for (String key :
                    projectSum.keySet()) {
                //1)获取项目的项目类型名称
                String projectTypeId = projectTypeRelationship.get(key);
                String projectTypeName = projectType.get(projectTypeId);
                System.out.println("project："+key+"的项目类型为:"+projectTypeId+"名称："+projectTypeName);
                //2)项目类型名称为key
                if (!record.contains(projectTypeName)) {
                    record.add(projectTypeName);
                    Map<String, Integer> projectTypeSum = new HashMap<>();
                    projectTypeSum.put(projectTypeName, projectSum.get(key));
                    projectTypeSumList.add(projectTypeSum);
                } else {
                    for (Map<String, Integer> projectTypeSum :
                            projectTypeSumList) {
                        for (String keyi :
                                projectTypeSum.keySet()) {
                            if (!keyi.equals(projectTypeName)) {
                                break;
                            } else {
                                int currentSum = projectTypeSum.get(projectTypeName);
                                currentSum += projectSum.get(key);
                                projectTypeSum.put(projectTypeName, currentSum);
                            }
                        }
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put("projectSumList",projectSumList);
        result.put("projectTypeSumList", projectTypeSumList);
        return result;
    }
}
