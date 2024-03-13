package com.learn.vm.reposiories;

import com.learn.vm.entities.Customer;
import com.learn.vm.entities.IPBlacklist;
import org.springframework.data.repository.CrudRepository;

public interface IPBlacklistRepository extends CrudRepository<IPBlacklist, Long> {

}
