package com.renjie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renjie.entity.User;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author renjie
 * @since 2020-11-12
 */
public interface UserService extends IService<User> {

    // 从数据库中取出用户信息
    User selectByUsername(String username);
}
