package vn.edu.qlhs.auth.service;

import org.springframework.stereotype.Service;
import vn.edu.qlhs.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.lockedUntil != null && attempt.lockedUntil.isAfter(Instant.now())) {
            throw new BusinessException("Đăng nhập quá nhiều lần. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void onSuccess(String key) {
        attempts.remove(key);
    }

    public void onFailure(String key) {
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plusSeconds(LOCK_MINUTES * 60);
            attempt.count = 0;
        }
    }

    private static class Attempt {
        private int count;
        private Instant lockedUntil;
    }
}
