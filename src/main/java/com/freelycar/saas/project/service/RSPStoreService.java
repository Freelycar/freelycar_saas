package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.model.RspStoreModel;
import com.freelycar.saas.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 14:55
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RSPStoreService {
    private StoreService storeService;

    private StoreRepository storeRepository;

    private StaffRepository staffRepository;

    private RspStaffStoreRepository rspStaffStoreRepository;

    private RSPStoreSortRepository rspStoreSortRepository;

    @Autowired
    public void setRspStoreSortRepository(RSPStoreSortRepository rspStoreSortRepository) {
        this.rspStoreSortRepository = rspStoreSortRepository;
    }

    @Autowired
    public void setStoreRepository(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Autowired
    public void setStoreService(StoreService storeService) {
        this.storeService = storeService;
    }


    /**
     * 服务商下全部网点列表
     * 包含：网点是否开通isArk功能（服务商是否服务于该网点）
     *
     * @param rspId
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    public Page<RspStoreModel> list(String rspId, String name, Integer currentPage, Integer pageSize) {
        Page<Store> storePage = storeRepository.findStoreByDelStatusAndNameContainingOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), name, PageableTools.basicPage(currentPage, pageSize));
        List<RspStoreModel> rspStoreModelList = new ArrayList<>();
        Set<String> storeIdSet = rspStoreSortRepository.findStoreIdByRspId(rspId);
        for (Store store :
                storePage.getContent()) {
            boolean isArk = storeIdSet.contains(store.getId());
            RspStoreModel model = new RspStoreModel();
            model.setId(store.getId());
            model.setName(store.getName());
            model.setAddress(store.getAddress());
            model.setIsArk(isArk);
            rspStoreModelList.add(model);
        }
        Pageable pageable = storePage.getPageable();
        Page<RspStoreModel> rspStorePage = new PageImpl(rspStoreModelList, pageable, storePage.getTotalElements());
        return rspStorePage;
    }

    /**
     * 获取服务商下开通智能柜功能的网点列表
     *
     * @param rspId
     * @return
     */
    public List<Store> listStore(String rspId) {
        Set<String> storeIdSet = rspStoreSortRepository.findStoreIdByRspId(rspId);
        List<Store> storeList = storeRepository.findByDelStatusAndIdIn(Constants.DelStatus.NORMAL.isValue(), new ArrayList<>(storeIdSet));
        return storeList;
    }

    /**
     * 服务商开通网点的智能柜功能
     * +同时添加服务商在网点下的排序
     *
     * @param ids   网点id
     * @param rspId 服务商id
     * @return
     */
    public ResultJsonObject openArk(String[] ids, String rspId) {
        //开通网点功能并排序
        for (int i = 0; i < ids.length; i++) {
            String storeId = ids[i];
            List<RSPStoreSort> sortList = rspStoreSortRepository.findByStoreIdAndRspId(storeId, rspId);
            if (sortList == null || sortList.size() == 0) {
                RSPStoreSort sort = new RSPStoreSort();
                sort.setRspId(rspId);
                sort.setStoreId(storeId);
                sort.setSort(this.generateSort(ids[i]));
                rspStoreSortRepository.saveAndFlush(sort);
            }
        }
        return ResultJsonObject.getDefaultResult(null);
    }

    /**
     * 生成网点下服务商的排序
     *
     * @param storeId
     * @return
     */
    private synchronized BigInteger generateSort(String storeId) {
        RSPStoreSort storeSort = rspStoreSortRepository.findTopByStoreIdOrderBySortDesc(storeId);
        if (null == storeSort) {
            return new BigInteger("10");
        }
        return storeSort.getSort().add(new BigInteger("10"));
    }

    /**
     * 关闭服务商相关网点智能柜功能：
     * + 删除网点下服务商的排序
     * 1.关闭服务商网点智能柜功能
     * 2.删除服务商下技师所选服务网点
     *
     * @param ids
     * @param rspId
     * @return
     */
    public ResultJsonObject closeArk(String[] ids, String rspId) {
        for (int i = 0; i < ids.length; i++) {
            rspStoreSortRepository.deleteByStoreIdAndRspId(ids[i], rspId);
        }
        //服务商下技师
        /*Set<String> idSet = new HashSet<>(Arrays.asList(ids));//待删除门店id
        List<Staff> staffList = staffRepository.findByDelStatusAndRspId(Constants.DelStatus.NORMAL.isValue(), rspId);
        if (staffList.size() > 0) {
            for (Staff staff :
                    staffList) {
                Set<String> storeIds = rspStaffStoreRepository.findByStaffId(staff.getId());
                for (String storeId :
                        storeIds) {
                    if (idSet.contains(storeId)) {
                        rspStaffStoreRepository.deleteByStaffIdAndStoreId(staff.getId(), storeId);
                    } else continue;
                }
            }
        }*/
        return ResultJsonObject.getDefaultResult(null);
    }
}
