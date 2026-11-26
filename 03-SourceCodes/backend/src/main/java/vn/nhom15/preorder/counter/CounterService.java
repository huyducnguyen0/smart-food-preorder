package vn.nhom15.preorder.counter;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.config.OrderingProperties;

/** Danh sách đơn cho quầy: đơn hôm nay còn đang xử lý. */
@Service
public class CounterService {

    private final CounterQueueRepository repository;
    private final OrderingProperties properties;
    private final Clock clock;

    public CounterService(CounterQueueRepository repository, OrderingProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CounterOrder> listOpenOrders() {
        // Khung bắt đầu trước hoặc đúng mốc này thì đã quá hạn nhận (giờ kết thúc + thời gian chờ ≤ bây giờ).
        java.time.LocalDateTime noShowBefore = java.time.LocalDateTime.now(clock)
                .minusMinutes(properties.slotMinutes())
                .minusMinutes(properties.noShowGraceMinutes());
        return repository.findOpenOrders(LocalDate.now(clock), properties.slotMinutes(), noShowBefore);
    }
}
