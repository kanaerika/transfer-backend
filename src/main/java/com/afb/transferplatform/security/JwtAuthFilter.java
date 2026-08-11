package com.afb.transferplatform.security;

import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.repository.AgentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AgentRepository agentRepository;

    public JwtAuthFilter(JwtService jwtService, AgentRepository agentRepository) {
        this.jwtService = jwtService;
        this.agentRepository = agentRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            handleAuthHeader(header, request);
        } else if (request.getRequestURI().startsWith("/api/")
                && !request.getRequestURI().startsWith("/api/auth")) {
            logger.warn("[JWT] Aucun header Authorization sur " + request.getRequestURI()
                    + " → 401 si endpoint protégé.");
        }
        filterChain.doFilter(request, response);
    }

    private void handleAuthHeader(@NonNull String header, @NonNull HttpServletRequest request) {
        try {
            Long agentId = jwtService.extractAgentId(header.substring(7));
            Agent agent = null;
            if (agentId != null) {
                agent = agentRepository.findById(agentId).orElse(null);
            }
            if (agent != null && (!agent.isActif() || !agent.compteActive()
                    || (agent.getPartenaire() != null && !agent.getPartenaire().isActif()))) {
                agent = null; // compte ou partenaire désactivé → accès refusé
            }
            if (agent == null) {
                logger.warn("[JWT] Token valide mais agent id=" + agentId
                        + " introuvable en base (base réinitialisée ?) → 401. "
                        + "Solution : recréer un compte et se reconnecter.");
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(agent, null,
                                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_" + agent.getRole())));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            // Jeton invalide ou expiré : la requête reste anonyme.
            logger.warn("[JWT] Token rejeté (" + e.getClass().getSimpleName() + ": "
                    + e.getMessage() + ") → 401. Solution : se reconnecter.");
        }
    }
}