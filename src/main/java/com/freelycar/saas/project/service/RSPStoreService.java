package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.permission.entity.SysUser;
import com.freelycar.saas.project.entity.RSPStore;
import com.freelycar.saas.project.entity.Store;
import com.freelycar.saas.project.model.RspStoreModel;
import com.freelycar.saas.project.model.StoreAccount;
import com.freelycar.saas.project.repository.RSPStoreRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public ResultJsonObject closeArk(String[] ids, String rspId) {
        List<RSPStore> rspStoreList = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            rspStoreList.addAll(rspStoreRepository.findByStoreIdAndRspId(ids[i], rspId));
        }
        rspStoreRepository.deleteAll(rspStoreList);
        return ResultJsonObject.getDefaultResult(null);
    }
}
