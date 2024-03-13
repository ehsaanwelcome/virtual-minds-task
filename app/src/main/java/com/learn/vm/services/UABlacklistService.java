package com.learn.vm.services;


import com.learn.vm.entities.UABlacklist;
import com.learn.vm.reposiories.UABlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UABlacklistService {
    private static final Logger LOG = LoggerFactory.getLogger(UABlacklistService.class);
    @Autowired final UABlacklistRepository uaBlacklistRepository;
    @Autowired
    private final RedisTemplate<String, Boolean> redisCache;
    public UABlacklistService(UABlacklistRepository uaBlacklistRepository, RedisTemplate<String, Boolean> redisCache) {
        this.uaBlacklistRepository = uaBlacklistRepository;
        this.redisCache = redisCache;

        this.cacheBlockedUas();
    }

    private String getBlockedUaCacheKey(String id) {
        return MessageFormat.format("ua#{0}", id);
    }

    private String getBlockedUasCacheFlagKey() {
        return "blockeduas";
    }

    //cache blocked uas for future requests
    @Cacheable(value = "blockeduas", key = "#ua")
    public Boolean findIfUaIsBlocked(String ua) {
        if (!redisCache.hasKey(getBlockedUasCacheFlagKey())) {
            cacheBlockedUas();
        }
        var isBlocked = redisCache.opsForValue().get(getBlockedUaCacheKey(ua));
        return isBlocked != null;
    }

    public void cacheBlockedUas() {
        LOG.info("{} cached", getBlockedUasCacheFlagKey());
        StreamSupport.stream(this.uaBlacklistRepository.findAll().spliterator(), false)
                .forEach(c -> {
                    redisCache.opsForValue().set(getBlockedUaCacheKey(c.getUa()), true);
                });
        this.redisCache.opsForValue().set(getBlockedUasCacheFlagKey(), true);
    }
}
