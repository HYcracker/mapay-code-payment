package com.mapay.repository;

import com.mapay.model.PayChannel;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayChannelRepository extends JpaRepository<PayChannel, Long> {
    List<PayChannel> findByAccountId(Long accountId);
    List<PayChannel> findByAccountIdInAndStateOrderByLastTimeAsc(Collection<Long> accountIds, Integer state);
}
