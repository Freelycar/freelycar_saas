package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.SortDto;
import com.freelycar.saas.project.entity.Partner;
import com.freelycar.saas.project.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author tangwei - Toby
 * @date 2019-08-07
 * @email toby911115@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    public Partner save(Partner partner) {
        return partnerRepository.save(partner);
    }

    public long countAll() {
        return partnerRepository.count();
    }

    public Page<Partner> list(Integer currentPage, Integer pageSize) {
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize, new SortDto("asc", "id"));
        return partnerRepository.findAll(pageable);
    }
}
