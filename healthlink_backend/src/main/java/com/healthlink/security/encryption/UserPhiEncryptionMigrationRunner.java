package com.healthlink.security.encryption;

import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time migration runner to encrypt legacy plaintext PHI fields (firstName,
 * lastName, phoneNumber).
 * Detects plaintext by absence of ENC: prefix; rewrites field to trigger
 * converter encryption.
 * Safe to run idempotently.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPhiEncryptionMigrationRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long total = userRepository.count();
        long migrated = 0;
        for (User u : userRepository.findAll()) {
            boolean changed = false;
            if (needsEncrypt(u.getFirstName())) {
                u.setFirstName(stripPrefix(u.getFirstName()));
                changed = true;
            }
            if (needsEncrypt(u.getLastName())) {
                u.setLastName(stripPrefix(u.getLastName()));
                changed = true;
            }
            if (needsEncrypt(u.getPhoneNumber())) {
                u.setPhoneNumber(stripPrefix(u.getPhoneNumber()));
                changed = true;
            }
            if (changed) {
                userRepository.save(u); // triggers converter adding ENC:
                migrated++;
            }
        }
        if (migrated > 0) {
            log.info("PHI encryption migration complete. Migrated {}/{} user records.", migrated, total);
        } else {
            log.info("PHI encryption migration skipped; no legacy plaintext records detected ({} users).", total);
        }
    }

    private boolean needsEncrypt(String value) {
        return value != null && !value.startsWith("ENC:");
    }

    private String stripPrefix(String value) {
        return value == null ? null : (value.startsWith("ENC:") ? value.substring(4) : value);
    }
}