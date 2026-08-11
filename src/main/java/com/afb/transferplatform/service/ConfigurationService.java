package com.afb.transferplatform.service;

import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.Configuration;
import com.afb.transferplatform.repository.ConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Fournit et met à jour la configuration globale.
 *
 * Le plafond est mis en cache mémoire car il est lu à chaque contrôle de
 * transfert. La ligne unique (id=1) est créée au démarrage à partir de la
 * valeur d'amorçage définie dans application.yml.
 */
@Service
public class ConfigurationService {

    private static final Long ID = 1L;

    private final ConfigurationRepository repo;
    private final long plafondAmorcage;

    /** Cache mémoire du plafond, rafraîchi à chaque modification. */
    private volatile long plafondCache;

    public ConfigurationService(ConfigurationRepository repo,
                                @Value("${app.plafond-mensuel:1000000}") long plafondAmorcage) {
        this.repo = repo;
        this.plafondAmorcage = plafondAmorcage;
    }

    @PostConstruct
    @Transactional
    public void initialiser() {
        Configuration config = repo.findById(ID).orElseGet(() -> {
            Configuration c = new Configuration();
            c.setId(ID);
            c.setPlafondMensuel(plafondAmorcage);
            c.setModifieLe(Instant.now());
            c.setModifiePar("système");
            return repo.save(c);
        });
        this.plafondCache = config.getPlafondMensuel();
    }

    /** Lecture ultra-rapide, utilisée par le contrôle des transferts. */
    public long plafondMensuel() {
        return plafondCache;
    }

    public Configuration lire() {
        return repo.findById(ID).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Configuration non initialisée."));
    }

    /** Plafond partagé par tous les partenaires : seul l'admin Afriland peut le modifier. */
    @Transactional
    public Configuration modifierPlafond(long nouveauPlafond, Agent admin) {
        if (!admin.estAdminAfriland()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Réservé à l'administrateur Afriland.");
        }
        if (nouveauPlafond <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le plafond doit être un montant positif.");
        }
        Configuration config = lire();
        config.setPlafondMensuel(nouveauPlafond);
        config.setModifiePar(admin.getNomComplet());
        config.setModifieLe(Instant.now());
        repo.save(config);

        this.plafondCache = nouveauPlafond; // rafraîchit le cache immédiatement
        return config;
    }
}