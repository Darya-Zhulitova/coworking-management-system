package com.hse.adminservice.security;

import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminUserRepository.findByEmailAndArchivedFalse(username).orElseThrow(() -> new UsernameNotFoundException("Admin user not found"));

        return new User(admin.getEmail(), admin.getPasswordHash(), admin.getActive(), true, true, !admin.getArchived(), List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name())));
    }
}