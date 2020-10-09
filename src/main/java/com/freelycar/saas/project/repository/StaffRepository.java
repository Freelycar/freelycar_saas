package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author tangwei - Toby
 * @date 2018/10/17
 * @email toby911115@gmail.com
 */
public interface StaffRepository extends JpaRepository<Staff, String> {
    @Query(value = "select * from staff where id != :id and storeId = :storeId and delStatus = 0 and name = :name", nativeQuery = true)
    List<Staff> checkRepeatName(String id, String name, String storeId);

    @Query(value = "select * from staff where storeId = :storeId and delStatus = 0 and name = :name", nativeQuery = true)
    List<Staff> checkRepeatName(String name, String storeId);


    Page<Staff> findAllByDelStatusAndStoreIdAndIdContainingAndNameContaining(boolean delStatus, String storeId, String id, String name, Pageable pageable);

    Page<Staff> findAllByDelStatusAndRspIdAndNameContaining(boolean delStatus, String rspId, String name, Pageable pageable);

    Page<Staff> findAllByDelStatusAndRspIdAndNameContainingAndIdIn(boolean delStatus, String rspId, String name, Set<String> ids, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update staff set delStatus = 1 where id=:id", nativeQuery = true)
    int delById(String id);

    @Query(value = "select * from staff where account=:account and id!=:id and isArk = 1", nativeQuery = true)
    List<Staff> checkRepeatAccount(String account, String id);

    @Query(value = "select * from staff where account=:account", nativeQuery = true)
    List<Staff> checkRepeatAccount(String account);

    Staff findTopByAccountAndPasswordAndDelStatus(String account, String password, boolean delStatus);


    List<Staff> findAllByDelStatusAndIsArkAndStoreId(boolean delStatus, boolean isArk, String storeId);

    /**
     * 查找某个手机号对应的所有员工信息（理应只有storeId不同）
     *
     * @param phone
     * @param delStatus
     * @return
     */
    List<Staff> findAllByPhoneAndDelStatus(String phone, boolean delStatus);

    List<Staff> findAllByPhoneAndDelStatusAndIsArk(String phone, boolean delStatus, boolean isArk);

    Staff findTopByStoreIdAndPhoneAndDelStatusAndIsArk(String storeId, String phone, boolean delStatus, boolean isArk);


    /**
     * 验证门店中手机号唯一性（排除数据本身）
     *
     * @param id
     * @param phone
     * @param storeId
     * @return
     */
    @Query(value = "select * from staff where id != :id and storeId = :storeId and delStatus = 0 and phone = :phone", nativeQuery = true)
    List<Staff> checkRepeatPhone(@Param("id") String id, @Param("phone") String phone, @Param("storeId") String storeId);


    /**
     * 验证服务商中员工手机号唯一（排除数据本身）
     *
     * @param id
     * @param phone
     * @param rspId
     * @return
     */
    @Query(value = "select * from staff where id != :id and rspId = :rspId and delStatus = 0 and phone = :phone", nativeQuery = true)
    List<Staff> checkRepeatRspStaffPhone(@Param("id") String id, @Param("phone") String phone, @Param("rspId") String rspId);

    /**
     * 验证门店中手机号唯一性（不排除数据本身）
     *
     * @param phone
     * @param storeId
     * @return
     */
    @Query(value = "select * from staff where storeId = :storeId and delStatus = 0 and phone = :phone", nativeQuery = true)
    List<Staff> checkRepeatPhone(@Param("phone") String phone, @Param("storeId") String storeId);

    /**
     * 验证服务商中员工手机号唯一（不排除数据本身）
     *
     * @param phone
     * @param rspId
     * @return
     */
    @Query(value = "select * from staff where rspId = :rspId and delStatus = 0 and phone = :phone", nativeQuery = true)
    List<Staff> checkRepeatRspStaffPhone(@Param("phone") String phone, @Param("rspId") String rspId);

    List<Staff> findByNameAndDelStatusAndIsArk(String name, boolean delStatus, boolean isArk);

    Optional<Staff> findByStoreIdAndDelStatusAndPhone(String storeId, boolean delStatus, String phone);

}
