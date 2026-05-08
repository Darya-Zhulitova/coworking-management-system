package com.hse.adminservice.common.security;

import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPrincipalUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminRepository.findByEmailIgnoreCase(username)
                .<UserDetails>map(admin -> new AuthenticatedAdminPrincipal(
                        admin.getId(),
                        admin.getEmail(),
                        admin.getPasswordHash(),
                        true,
                        true,
                        true,
                        true,
                        List.of()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Admin subject not found"));
    }
}
