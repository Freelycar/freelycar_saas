package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.RSPStore;
import com.freelycar.saas.project.entity.RspStaffStore;
import com.freelycar.saas.project.entity.Staff;
import com.freelycar.saas.project.entity.Store;
import com.freelycar.saas.project.model.RspStoreModel;
import com.freelycar.saas.project.repository.RSPStoreRepository;
import com.freelycar.saas.project.repository.RspStaffStoreRepository;
import com.freelycar.saas.project.repository.StaffRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private RSPStoreRepository rspStoreRepository;

    private StoreRepository storeRepository;

    private StaffRepository staffRepository;

    private RspStaffStoreRepository rspStaffStoreRepository;

    @Autowired
    public void setStoreRepository(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Autowired
    public void setStoreService(StoreService storeService) {
        this.storeService = storeService;
    }

    @Autowired
    public void setRspStoreRepository(RSPStoreRepository rspStoreRepository) {
        this.rspStoreRepository = rspStoreRepository;
    }


    public Page<RspStoreModel> list(String rspId, String name, Integer currentPage, Integer pageSize) {
        Page<Store> storePage = storeRepository.findStoreByDelStatusAndNameContainingOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), name, PageableTools.basicPage(currentPage, pageSize));
        List<RspStoreModel> rspStoreModelList = new ArrayList<>();
        Set<String> storeIdSet = rspStoreRepository.findByRspId(rspId);
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
        Set<String> storeIdSet = rspStoreRepository.findByRspId(rspId);
        List<Store> storeList = storeRepository.findByDelStatusAndIdIn(Constants.DelStatus.NORMAL.isValue(), new ArrayList<>(storeIdSet));
        return storeList;
    }

    public ResultJsonObject openArk(String[] ids, String rspId) {
        List<RSPStore> rspStoreList = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            RSPStore rsp_store = new RSPStore();
            rsp_store.setRspId(rspId);
            rsp_store.setStoreId(ids[i]);
            rspStoreList.add(rsp_store);
        }
        rspStoreRepository.saveAll(rspStoreList);
        return ResultJsonObject.getDefaultResult(null);
    }

    /**
     * 关闭服务商相关网点智能柜功能：
     * 1.关闭服务商网点智能柜功能
     * 2.删除服务商下技师所选服务网点
     *
     * @param ids
     * @param rspId
     * @return
     */
    public ResultJsonObject closeArk(String[] ids, String rspId) {
        List<RSPStore> rspStoreList = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            rspStoreList.addAll(rspStoreRepository.findByStoreIdAndRspId(ids[i], rspId));
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
        rspStoreRepository.deleteAll(rspStoreList);
        return ResultJsonObject.getDefaultResult(null);
    }
}
