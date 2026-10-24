package vn.nhom15.preorder.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/** Đồng hồ test chỉnh được giờ, để một test đặt đơn lúc sớm rồi xem hàng chờ lúc muộn hơn. */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private final AtomicReference<Instant> now;

    public MutableClock(ZoneId zone, LocalDateTime start) {
        this.zone = zone;
        this.now = new AtomicReference<>(start.atZone(zone).toInstant());
    }

    /** Đặt giờ hiện tại theo giờ cửa hàng. */
    public void set(LocalDateTime dateTime) {
        now.set(dateTime.atZone(zone).toInstant());
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        throw new UnsupportedOperationException("Đồng hồ test chỉ dùng một múi giờ");
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
