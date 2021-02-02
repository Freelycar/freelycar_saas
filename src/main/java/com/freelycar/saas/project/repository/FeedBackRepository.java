package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.FeedBack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: pyt
 * @Date: 2021/1/15 10:09
 * @Description:
 */
@Repository
public interface FeedBackRepository extends JpaRepository<FeedBack, Long> {
    Page<FeedBack> findByFeedBackTypesContaining(String feedBackTypes, Pageable pageable);
}
