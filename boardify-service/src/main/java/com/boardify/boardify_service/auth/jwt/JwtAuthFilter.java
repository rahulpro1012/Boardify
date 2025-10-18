package com.boardify.boardify_service.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private final JwtService jwt;
    private final UserDetailsService uds;

    public JwtAuthFilter(JwtService jwt, UserDetailsService uds) {
        this.jwt = jwt;
        this.uds = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // 1️⃣ Skip auth endpoints (login/register)
        String path = req.getRequestURI();
        if (path.startsWith("/auth/")) {
            chain.doFilter(req, res);
            return;
        }

        // 2️⃣ Check Authorization header
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res); // let SecurityConfig handle unauthorized requests
            return;
        }

        String token = header.substring(7);
        try {
            // 3️⃣ Check blacklist - moved before any validation
            if (jwt.isTokenBlacklisted(token)) {
                logger.warn("Attempted to use blacklisted token");
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been invalidated. Please log in again.");
                return;
            }

            // 4️⃣ Validate token and set authentication
            String email;
            try {
                email = jwt.validateAndGetSubject(token);
            } catch (ExpiredJwtException e) {
                // If token is expired, add it to blacklist before throwing
                logger.info("Token expired, adding to blacklist");
                jwt.blacklistToken(token, e.getClaims().getExpiration().getTime());
                throw e;
            }
            UserDetails user = uds.loadUserByUsername(email);
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (ExpiredJwtException e) {
            logger.error("Token expired: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
            return;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (SignatureException e) {
            logger.error("JWT signature does not match: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT signature");
            return;
        } catch (IllegalArgumentException e) {
            logger.error("JWT token compact of handler are invalid: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "User not found");
            return;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
            return;
        }

        // 5️⃣ Continue only if authentication succeeded
        chain.doFilter(req, res);
    }
}
