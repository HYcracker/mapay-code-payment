package com.mapay.repository;

import com.mapay.model.PaymentOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByOutTradeNo(String outTradeNo);
    Optional<PaymentOrder> findByConfirmToken(String confirmToken);
    boolean existsByOutTradeNo(String outTradeNo);
    List<PaymentOrder> findByStateAndCloseTimeAfter(Integer state, LocalDateTime now);
    List<PaymentOrder> findByAidAndCidAndTypeAndStateAndCloseTimeAfter(Long aid, Long cid, String type, Integer state, LocalDateTime now);
    List<PaymentOrder> findByAidAndStateAndCloseTimeAfter(Long aid, Integer state, LocalDateTime now);
    List<PaymentOrder> findByStateAndCloseTimeBefore(Integer state, LocalDateTime now);
    List<PaymentOrder> findByCreateTimeBetweenAndState(LocalDateTime start, LocalDateTime end, Integer state);
    Page<PaymentOrder> findAllByOrderByIdDesc(Pageable pageable);

    /** 按天分组统计已支付订单 — 返回 [日期, 订单数, 金额] */
    @Query("SELECT CAST(o.createTime AS LocalDate), COUNT(o), COALESCE(SUM(o.reallyPrice), 0) FROM PaymentOrder o WHERE o.state = 1 AND o.createTime BETWEEN :start AND :end GROUP BY CAST(o.createTime AS LocalDate) ORDER BY CAST(o.createTime AS LocalDate)")
    List<Object[]> dailyStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按支付类型分组统计已支付订单 — 返回 [类型, 订单数, 金额] */
    @Query("SELECT o.type, COUNT(o), COALESCE(SUM(o.reallyPrice), 0) FROM PaymentOrder o WHERE o.state = 1 AND o.createTime BETWEEN :start AND :end GROUP BY o.type ORDER BY SUM(o.reallyPrice) DESC")
    List<Object[]> typeStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按小时分组统计已支付订单 — 返回 [小时(0-23), 订单数] */
    @Query("SELECT HOUR(o.createTime), COUNT(o) FROM PaymentOrder o WHERE o.state = 1 AND o.createTime BETWEEN :start AND :end GROUP BY HOUR(o.createTime) ORDER BY HOUR(o.createTime)")
    List<Object[]> hourlyStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
