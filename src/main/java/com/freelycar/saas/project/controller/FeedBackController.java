package com.freelycar.saas.project.controller;

import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.project.entity.FeedBack;
import com.freelycar.saas.project.service.FeedBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: pyt
 * @Date: 2021/1/15 14:24
 * @Description:
 */
@RestController
@RequestMapping("/feedback")
public class FeedBackController {
    @Autowired
    private FeedBackService feedBackService;

    @PostMapping("/add")
    public ResultJsonObject add(@RequestBody FeedBack feedBack) {
        try {
            feedBackService.createFeedBack(feedBack);
            return ResultJsonObject.getDefaultResult(null);
        } catch (ArgumentMissingException e) {
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResultJsonObject list(
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) boolean sort
    ) {
        if (StringUtils.isEmpty(StringUtils.trimWhitespace(type))) {
            type = "";
        }
        Page<FeedBack> feedBackPage = feedBackService.list(currentPage, pageSize, type, sort);
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(feedBackPage));
    }
}
