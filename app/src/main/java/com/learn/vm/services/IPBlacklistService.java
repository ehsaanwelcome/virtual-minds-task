package com.learn.vm.services;


import com.learn.vm.entities.IPBlacklist;
import com.learn.vm.reposiories.IPBlacklistRepository;
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
public class IPBlacklistService {
    private static final Logger LOG = LoggerFactory.getLogger(IPBlacklistService.class);
    @Autowired final IPBlacklistRepository ipBlacklistRepository;
    @Autowired
    private final RedisTemplate<String, Boolean> redisCache;
    public IPBlacklistService(IPBlacklistRepository ipBlacklistRepository, RedisTemplate<String, Boolean> redisCache) {
        this.ipBlacklistRepository = ipBlacklistRepository;
        this.redisCache = redisCache;

        this.cacheBlockedIps();
    }

    private String getBlockedIpCacheKey(long id) {
        return MessageFormat.format("ip#{0}", String.valueOf(id));
    }

    private String getBlockedIpsCacheFlagKey() {
        return "blockedips";
    }

    //cache blocked ips for future requests
    @Cacheable(value = "blockedips", key = "#id")
    public boolean findIfIpIsBlocked(long id) {
        if (!redisCache.hasKey(getBlockedIpsCacheFlagKey())) {
            cacheBlockedIps();
        }
        var isBlocked = redisCache.opsForValue().get(getBlockedIpCacheKey(id));
        return isBlocked != null;
    }

    public void cacheBlockedIps() {
        LOG.info("{} cached", getBlockedIpsCacheFlagKey());
        StreamSupport.stream(this.ipBlacklistRepository.findAll().spliterator(), false)
                .forEach(c -> {
                    redisCache.opsForValue().set(getBlockedIpCacheKey(c.getIp()), true);
                });
        this.redisCache.opsForValue().set(getBlockedIpsCacheFlagKey(), true);
    }
}
