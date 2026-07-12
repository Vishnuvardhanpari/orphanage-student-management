package com.orphanage.oms.security;

import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.RoleName;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal wrapping an application {@link User}.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final RoleName role;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().getName();
        this.enabled = user.isEnabled();
        this.accountNonLocked = user.isAccountNonLocked() && !user.isCurrentlyLocked();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public RoleName getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
