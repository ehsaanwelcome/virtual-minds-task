package com.learn.vm.services;


import com.learn.vm.entities.Customer;
import com.learn.vm.reposiories.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CustomerService {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);
    @Autowired
    final CustomerRepository customerRepository;
    @Autowired
    private final RedisTemplate<String, Boolean> redisCache;

    public CustomerService(CustomerRepository customerRepository, RedisTemplate<String, Boolean> redisCache) {
        this.customerRepository = customerRepository;
        this.redisCache = redisCache;

        this.cacheCustomers();
    }

    private String getCustomerCacheKey(long id) {
        return MessageFormat.format("cus#{0}", String.valueOf(id));
    }

    private String getCustomersCacheFlagKey() {
        return "customers";
    }

    //cache customers for future requests
    @Cacheable(value = "customers", key = "#id")
    public Boolean findIfCustomerIsActive(long id) {
        if (!redisCache.hasKey(getCustomersCacheFlagKey())) {
            cacheCustomers();
        }
        return redisCache.opsForValue().get(getCustomerCacheKey(id));
    }

    public void cacheCustomers() {
        LOG.info("{} cached", getCustomersCacheFlagKey());
        StreamSupport.stream(this.customerRepository.findAll().spliterator(), false)
                .forEach(c -> {
                    redisCache.opsForValue().set(getCustomerCacheKey(c.getId()), c.isActive());
                });
        this.redisCache.opsForValue().set(getCustomersCacheFlagKey(), true);
    }


    //        var customers = new ArrayList<Customer>();
//        try (var conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/postgres", "postgres", "test");
//             var stmt = conn.createStatement()) {
//            ResultSet rs = stmt.executeQuery("select id, active from customer;");
//
//            // Step 4: Process the ResultSet object.
//            while (rs.next()) {
//                long id = rs.getLong("id");
//                boolean active = rs.getBoolean("active");
//
//                customers.add(Customer.builder().id(id).active(active).build());
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
}
