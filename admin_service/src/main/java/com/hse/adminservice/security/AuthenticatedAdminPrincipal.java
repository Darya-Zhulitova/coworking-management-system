package com.hse.adminservice.security;

import com.hse.adminservice.entity.AdminPrincipalType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class AuthenticatedAdminPrincipal extends User {

    private final Long subjectId;
    private final AdminPrincipalType principalType;

    public AuthenticatedAdminPrincipal(
            Long subjectId,
            AdminPrincipalType principalType,
            String username,
            String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.subjectId = subjectId;
        this.principalType = principalType;
    }
}
