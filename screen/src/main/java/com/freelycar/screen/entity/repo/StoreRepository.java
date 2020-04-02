package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author pyt
 * @date 2020/4/2 12:01
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByDelStatusAndIdIn(boolean delStatus, List<String> id);
}
