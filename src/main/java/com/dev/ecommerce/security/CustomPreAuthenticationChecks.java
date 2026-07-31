package com.dev.ecommerce.security;

import com.dev.ecommerce.exception.AccountLockedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * Replaces Spring Security's default pre-authentication checks
 * (org.springframework.security.authentication.dao.DefaultPreAuthenticationChecks)
 * so that a locked account throws AccountLockedException carrying the lock
 * expiry time, instead of a bare LockedException with no detail.
 *
 * Checks run in the same order as the default: locked -> disabled -> expired.
 * This only runs BEFORE password verification (DaoAuthenticationProvider
 * calls preAuthenticationChecks before comparing credentials), so a locked
 * account is rejected regardless of whether the password is correct.
 */
public class CustomPreAuthenticationChecks implements UserDetailsChecker {

    @Override
    public void check(UserDetails user) {
        if (!(user instanceof UserPrincipal principal)) {
            return;
        }

        if (!principal.isAccountNonLocked()) {
            throw new AccountLockedException("Account is locked", principal.getLockedUntil());
        }

        if (!principal.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!principal.isAccountNonExpired()) {
            throw new AccountExpiredException("Account has expired");
        }
    }
}