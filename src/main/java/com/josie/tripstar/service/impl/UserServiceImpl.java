package com.josie.tripstar.service.impl;

import com.josie.tripstar.service.UserService;

import com.josie.tripstar.mapper.UserMapper;
import com.josie.tripstar.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper mapper;
    /** 唯一索引保证同一个匿名标识并发访问时只创建一个用户。 */
    @Override
    public AppUser getOrCreate(String publicId) {
        mapper.insertIfAbsent(publicId);
        return mapper.find(publicId);
    }
}
