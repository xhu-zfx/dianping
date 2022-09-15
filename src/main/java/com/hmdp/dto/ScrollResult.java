package com.hmdp.dto;

import lombok.Data;

import java.util.List;
/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
