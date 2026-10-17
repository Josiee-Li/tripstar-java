package com.josie.tripstar.service;

import com.josie.tripstar.entity.AppUser;

/** 用户业务接口。 */
public interface UserService {
    /** 获取或创建匿名用户。 */
    AppUser getOrCreate(String publicId);
}
