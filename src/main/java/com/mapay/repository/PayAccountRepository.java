package com.mapay.repository;

import com.mapay.model.PayAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayAccountRepository extends JpaRepository<PayAccount, Long> {
    List<PayAccount> findByPidAndState(Long pid, Integer state);
    List<PayAccount> findByPid(Long pid);
}
