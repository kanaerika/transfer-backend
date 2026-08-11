package com.afb.transferplatform.entity;

import jakarta.persistence.*;

/** Partenaire de distribution (Financial House, Express Union, Julie Voyage...). */
@Entity
@Table(name = "partenaires")
public class Partenaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    /** Email de contact du partenaire (celui de son administrateur) */
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean actif = true;

    public Partenaire() {}
    public Partenaire(String nom, String email) { this.nom = nom; this.email = email; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
