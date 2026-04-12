package com.hse.userservice.config;

import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.domain.user.User;
import com.hse.userservice.repository.MembershipRepository;
import com.hse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MockUserDataRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (membershipRepository.count() > 0) {
            return;
        }

        User primary = seedUser("user@test.test", "Дарья", "Разработчик");
        User second = seedUser("user2@test.test", "Иван", "Дизайнер");
        User third = seedUser("user3@test.test", "Дмитрий", "Менеджер");

        Membership active = seedMembership(
                primary.getId(),
                1L,
                MembershipStatus.ACTIVE,
                LocalDateTime.now().minusDays(40)
        );
        Membership secondActive = seedMembership(
                second.getId(),
                1L,
                MembershipStatus.PENDING,
                LocalDateTime.now().minusDays(25)
        );
        Membership thirdActive = seedMembership(
                third.getId(),
                1L,
                MembershipStatus.BLOCKED,
                LocalDateTime.now().minusDays(18)
        );
        Membership pending = seedMembership(
                primary.getId(),
                2L,
                MembershipStatus.PENDING,
                LocalDateTime.now().minusDays(12)
        );
        Membership blocked = seedMembership(
                primary.getId(),
                3L,
                MembershipStatus.BLOCKED,
                LocalDateTime.now().minusDays(65)
        );
    }

    private User seedUser(String email, String name, String description) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setDescription(description);
            user.setPasswordHash(passwordEncoder.encode("123456"));
            return userRepository.save(user);
        });
    }

    private Membership seedMembership(Long userId, Long coworkingId, MembershipStatus status, LocalDateTime createdAt) {
        Membership membership = membershipRepository.findByUserIdAndCoworkingId(userId, coworkingId).orElseGet(
                Membership::new);
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(status);
        membership.setCreatedAt(createdAt);
        membership.setApprovedAt(status == MembershipStatus.ACTIVE ? createdAt.plusHours(1) : null);
        membership.setBlockedAt(status == MembershipStatus.BLOCKED ? createdAt.plusDays(1) : null);
        return membershipRepository.save(membership);
    }
}
