package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService iFollowService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,@PathVariable("isFollow") Boolean isFollow){
        return iFollowService.follow(followUserId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId){
        return iFollowService.isFollow(followUserId);
    }
    @GetMapping("/common/{id}")
    public Result commonFollows(@PathVariable("id") Long followUserId) {
        return iFollowService.commonFollows(followUserId);
    }
}
