package com.afb.transferplatform.config;

import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.Partenaire;
import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.repository.PartenaireRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Premier démarrage : crée le partenaire Afriland et son administrateur.
 * Cet admin a le même rôle et les mêmes fonctionnalités que tout autre admin ;
 * son seul particularisme métier est d'être le premier à onboarder les autres.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initialiser(AgentRepository agents,
                                  PartenaireRepository partenaires,
                                  PasswordEncoder encoder) {
        return args -> {
            Partenaire afriland = partenaires.findAll().stream()
                    .filter(p -> p.getNom().equalsIgnoreCase("Afriland First Bank"))
                    .findFirst()
                    .orElseGet(() -> partenaires.save(
                            new Partenaire("Afriland First Bank", "contact@afriland.cm")));

            if (!afriland.isActif()) {
                afriland.setActif(true);
                partenaires.save(afriland);
                log.info("Partenaire Afriland activé");
            }

            agents.findByEmail("admin@afriland.cm").ifPresentOrElse(
                    admin -> {
                        boolean updated = false;
                        if (!admin.isActif()) {
                            admin.setActif(true);
                            updated = true;
                        }
                        if (!admin.isInvitationAcceptee()) {
                            admin.setInvitationAcceptee(true);
                            updated = true;
                        }
                        if (admin.isFirstLogin()) {
                            admin.setFirstLogin(false);
                            updated = true;
                        }
                        if (updated) {
                            agents.save(admin);
                            log.info("ADMIN Afriland activé et mis à jour : admin@afriland.cm");
                        }
                    },
                    () -> {
                        Agent admin = new Agent();
                        admin.setNomComplet("Administrateur Afriland");
                        admin.setEmail("admin@afriland.cm");
                        admin.setRole("ADMIN");
                        admin.setPartenaire(afriland);
                        admin.setMotDePasse(encoder.encode("Admin@2026"));
                        admin.setAgence("SIEGE");
                        admin.setCodeAgent("00");
                        admin.setActif(true);
                        admin.setInvitationAcceptee(true);
                        admin.setFirstLogin(false);

                        agents.save(admin);
                        log.info("ADMIN Afriland créé : admin@afriland.cm");
                    }
            );
        };
    }
}