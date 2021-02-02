package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.FeedBackImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: pyt
 * @Date: 2021/1/15 10:10
 * @Description:
 */
@Repository
public interface FeedBackImgRepository extends JpaRepository<FeedBackImg, Long> {
}
