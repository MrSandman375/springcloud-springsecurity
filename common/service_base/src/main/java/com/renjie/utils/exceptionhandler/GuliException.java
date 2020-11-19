package com.renjie.utils.exceptionhandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Fan
 * @Date 2020/11/18
 * @Description: 自定义异常使用的实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuliException extends RuntimeException{

    private Integer code;//状态码
    private String msg;//异常信息

}
