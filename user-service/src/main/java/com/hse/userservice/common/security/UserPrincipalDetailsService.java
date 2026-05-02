package com.hse.userservice.common.security;

import com.hse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPrincipalDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .<UserDetails>map(user -> new AuthenticatedUserPrincipal(user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        true,
                        true,
                        true,
                        true,
                        List.of()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
