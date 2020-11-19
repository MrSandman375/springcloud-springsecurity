package com.renjie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.renjie.entity.Permission;

import java.util.List;

/**
 * <p>
 * 权限 Mapper 接口
 * </p>
 *
 * @author renjie
 * @since 2020-11-12
 */
public interface PermissionMapper extends BaseMapper<Permission> {


    List<String> selectPermissionValueByUserId(String id);

    List<String> selectAllPermissionValue();

    List<Permission> selectPermissionByUserId(String userId);
}
