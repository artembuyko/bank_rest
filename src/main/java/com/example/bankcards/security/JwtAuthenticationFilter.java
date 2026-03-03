package com.example.bankcards.security;

import com.example.bankcards.entity.record.ParsedToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;
    private final CustomUserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Optional<String> token = extractToken(request);
        if(token.isEmpty()){
            filterChain.doFilter(request,response);
            return;
        }

        ParsedToken parsedToken = provider.parseToken(token.get());
        if(!parsedToken.valid()){
            handleError(response,parsedToken);
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken = buildAuthentication(request,parsedToken);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request,response);
    }

    private Optional<String> extractToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")){
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(HttpServletRequest request,ParsedToken parsedToken){
        CustomUserDetails user = userDetailsService.loadUserByUsername(parsedToken.subject());

        String role = parsedToken.role();
        GrantedAuthority authority = new SimpleGrantedAuthority(role);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(authority)
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

    private void handleError(HttpServletResponse response,ParsedToken parsedToken){
        try{
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String body = """
                    {"error":":%s","expired:"%b}
                    """.formatted(parsedToken.error(),parsedToken.expired());
            response.getWriter().write(body);
        } catch (IOException e) {
            log.error("Failed to send error", e);
        }
    }
}
