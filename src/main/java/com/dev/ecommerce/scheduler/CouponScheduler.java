package com.dev.ecommerce.scheduler;

import com.dev.ecommerce.entity.Coupon;
import com.dev.ecommerce.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponRepository couponRepository;

    @Scheduled(cron = "0 0 1 * * ?") // 1 AM daily
    @Transactional
    public void disableExpiredCoupons() {
        List<Coupon> expiredCoupons = couponRepository.findAll().stream()
                .filter(c -> !c.isActive() == false && c.isExpired())
                .toList();

        if (expiredCoupons.isEmpty()) return;

        for (Coupon coupon : expiredCoupons) {
            coupon.setActive(false);
        }
        couponRepository.saveAll(expiredCoupons);

        log.info("Disabled {} expired coupons", expiredCoupons.size());
    }
}
