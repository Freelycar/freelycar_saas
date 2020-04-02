package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author pyt
 * @date 2020/4/2 17:03
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project,String> {
    List<Project> findByDelStatusAndIdIn(boolean delStatus, Set<String> id);
}
