import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VelocityProviderImpl implements VelocityProvider {
    private static volatile VelocityProviderImpl velocityProvider;

    private static ConcurrentHashMap<String, List<Solution.Payment>> cache;
    private static ConcurrentHashMap<String, Lock> locks;


    public static VelocityProvider getProvider() {
        if (velocityProvider == null) {
            synchronized (VelocityProviderImpl.class) {
                if (velocityProvider == null) {
                    velocityProvider = new VelocityProviderImpl();
                }
            }
        }
        return velocityProvider;
    }

    private VelocityProviderImpl() {
        cache = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    @Override
    public int getCardUsageCount(Solution.Payment payment, Duration duration) {
        List<Solution.Payment> payments =
                 cache.getOrDefault(payment.getHashedCardNumber(), new CopyOnWriteArrayList<>());
        Lock lock = locks.computeIfAbsent(payment.getHashedCardNumber(), key -> new ReentrantLock());
        lock.lock();
        try {
            int usageCount = 0;

            for (Solution.Payment oldPayment : payments) {
                Duration currentDuration = Duration.between(oldPayment.getTimestamp(), payment.getTimestamp());
                // System.out.println("duration" + currentDuration.toMinutes());
                if (currentDuration.compareTo(duration) <= 0) {
                    usageCount++;
                }
            }
            invalidateExpiredPayments(payment);
            return usageCount;
        } finally {
            lock.unlock();
        }
    }

    private void invalidateExpiredPayments(Solution.Payment payment) {
        Lock lock = locks.computeIfAbsent(payment.getHashedCardNumber(), key -> new ReentrantLock());
        lock.lock();
        try {
            cache.compute(payment.getHashedCardNumber(), (key, value) -> {
                if (value == null) {
                    return null;
                }
                value.removeIf(oldPayment -> {
                    Duration currentDuration = Duration.between(oldPayment.getTimestamp(), payment.getTimestamp());
                    return currentDuration.compareTo(Duration.ofHours(10)) > 0;
                });
                return value;
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void registerPayment(Solution.Payment payment) {
        cache.compute(payment.getHashedCardNumber(), (key, value) -> {
            if (value == null) {
                value = new CopyOnWriteArrayList<>();
            }
            value.add(payment);
            return value;
        });
    }
}
