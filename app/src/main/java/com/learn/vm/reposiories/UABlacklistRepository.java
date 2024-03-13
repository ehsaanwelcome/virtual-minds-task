package com.learn.vm.reposiories;

import com.learn.vm.entities.Customer;
import com.learn.vm.entities.UABlacklist;
import org.springframework.data.repository.CrudRepository;

public interface UABlacklistRepository extends CrudRepository<UABlacklist, String> {

}
