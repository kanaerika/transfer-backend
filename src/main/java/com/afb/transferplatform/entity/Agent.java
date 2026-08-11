package com.afb.transferplatform.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Utilisateur de la plateforme. Deux rôles, mêmes fonctionnalités pour tous les ADMIN :
 *  - ADMIN : administrateur d'un partenaire (gère ses agents, voit ses statistiques).
 *            Scopé à son propre partenaire, quel que soit le partenaire.
 *            Seul particularisme : l'admin du partenaire "Afriland First Bank" est
 *            en plus habilité à onboarder les autres partenaires (voir estAdminAfriland()).
 *  - AGENT : opérateur (crée et suit les transferts de son partenaire)
 *
 * Tous les comptes (sauf l'admin Afriland initial) sont activés PAR INVITATION :
 * créés sans mot de passe, l'utilisateur le définit lui-même via un lien sécurisé.
 */
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomComplet;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String role = "AGENT";
    
    
    /** Jamais null en pratique : chaque admin (Afriland compris) est rattaché à un partenaire. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partenaire_id")
    private Partenaire partenaire;

    /** null tant que le compte n'a pas été activé par invitation */
    private String motDePasse;

    /** Activation/désactivation administrative du compte */
    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private boolean invitationAcceptee = false;
    /** true tant que l'utilisateur n'a pas remplacé son mot de passe temporaire */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean firstLogin = true;

    /** Jeton d'invitation (activation du compte / réinitialisation) */
    private String tokenInvitation;
    private Instant tokenExpiration;

    private String codeAgent;
    private String agence;
    private String telephone;

    public boolean isInvitationAcceptee() {
    return invitationAcceptee;
}

public void setInvitationAcceptee(boolean invitationAcceptee) {
    this.invitationAcceptee = invitationAcceptee;
}
    /** Le compte est utilisable une fois l'invitation validée par email. */
    public boolean compteActive() { return invitationAcceptee; }
    public boolean estAdmin() { return "ADMIN".equals(role); }
    public boolean estAgent() { return "AGENT".equals(role); }
    /** Seul l'admin du partenaire fondateur peut onboarder de nouveaux partenaires. */
    public boolean estAdminAfriland() {
        return estAdmin() && partenaire != null && "Afriland First Bank".equalsIgnoreCase(partenaire.getNom());
    }
    public String getNomPartenaire() { return partenaire != null ? partenaire.getNom() : "Afriland First Bank"; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Partenaire getPartenaire() { return partenaire; }
    public void setPartenaire(Partenaire partenaire) { this.partenaire = partenaire; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public String getTokenInvitation() { return tokenInvitation; }
    public void setTokenInvitation(String tokenInvitation) { this.tokenInvitation = tokenInvitation; }
    public Instant getTokenExpiration() { return tokenExpiration; }
    public void setTokenExpiration(Instant tokenExpiration) { this.tokenExpiration = tokenExpiration; }
    public String getCodeAgent() { return codeAgent; }
    public void setCodeAgent(String codeAgent) { this.codeAgent = codeAgent; }
    public String getAgence() { return agence; }
    public void setAgence(String agence) { this.agence = agence; }
    public String getTelephone() {
    return telephone;
     }

    public void setTelephone(String telephone) {
    this.telephone = telephone;
    }
    public boolean isFirstLogin() { return firstLogin; }
    public void setFirstLogin(boolean firstLogin) { this.firstLogin = firstLogin; }
}
