package com.hse.adminservice.security;

import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.repository.AdminUserRepository;
import com.hse.adminservice.repository.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPrincipalUserDetailsService implements UserDetailsService {

    private final SuperAdminRepository superAdminRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return superAdminRepository.findByEmailAndArchivedFalse(username)
                .<UserDetails>map(superAdmin -> new AuthenticatedAdminPrincipal(
                        superAdmin.getId(),
                        AdminPrincipalType.SUPERADMIN,
                        superAdmin.getEmail(),
                        superAdmin.getPasswordHash(),
                        superAdmin.getActive(),
                        true,
                        true,
                        !superAdmin.getArchived(),
                        List.of()
                ))
                .or(() -> adminUserRepository.findByEmailAndArchivedFalse(username)
                        .map(adminUser -> new AuthenticatedAdminPrincipal(
                                adminUser.getId(),
                                AdminPrincipalType.TENANT_ADMIN,
                                adminUser.getEmail(),
                                adminUser.getPasswordHash(),
                                adminUser.getActive(),
                                true,
                                true,
                                !adminUser.getArchived(),
                                List.of()
                        )))
                .orElseThrow(() -> new UsernameNotFoundException("Admin subject not found"));
    }
}
