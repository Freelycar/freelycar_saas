package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author pyt
 * @date 2020/4/2 16:59
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface ProjectTypeRepository extends JpaRepository<ProjectType, String> {
    List<ProjectType> findByIdIn(Set<String> id);
}
