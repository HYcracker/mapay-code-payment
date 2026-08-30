package com.mapay.repository;

import com.mapay.model.MailConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {
    Optional<MailConfig> findFirstByOrderByIdAsc();
}
