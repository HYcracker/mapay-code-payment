package com.mapay.repository;

import com.mapay.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPidAndState(Long pid, Integer state);
    Optional<User> findByPid(Long pid);

    @Query("select coalesce(max(u.pid), 999) from User u")
    Long maxPid();
}
