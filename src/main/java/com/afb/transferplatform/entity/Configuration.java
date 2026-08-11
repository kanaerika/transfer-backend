package com.afb.transferplatform.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Configuration globale de la plateforme (ligne unique, id = 1).
 * Contient les paramètres modifiables sans redéploiement, à commencer
 * par le plafond mensuel Hors CEMAC.
 */
@Entity
@Table(name = "configuration")
public class Configuration {

    @Id
    private Long id = 1L; // ligne unique

    /** Plafond mensuel Hors CEMAC, en FCFA. */
    @Column(nullable = false)
    private long plafondMensuel;

    /** Traçabilité de la dernière modification. */
    private String modifiePar;
    private Instant modifieLe;

    public Configuration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getPlafondMensuel() { return plafondMensuel; }
    public void setPlafondMensuel(long plafondMensuel) { this.plafondMensuel = plafondMensuel; }
    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String modifiePar) { this.modifiePar = modifiePar; }
    public Instant getModifieLe() { return modifieLe; }
    public void setModifieLe(Instant modifieLe) { this.modifieLe = modifieLe; }
}