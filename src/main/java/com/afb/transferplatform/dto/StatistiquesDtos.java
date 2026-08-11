package com.afb.transferplatform.dto;

import java.util.List;

/** Réponses des tableaux de bord statistiques, adaptées à chaque rôle. */
public class StatistiquesDtos {

    /** Une carte d'indicateur (KPI). */
    public record Kpi(String libelle, long valeur, String variante) {}

    /** Un point de série temporelle. */
    public record PointTemporel(String periode, long valeur) {}

    /** Une part dans un graphique (donut des opérations). */
    public record Part(String libelle, long valeur, String couleur) {}

    /** Une ligne de classement (top agents / partenaires). */
    public record Classement(Long id, String nom, long total) {}

    /** Un transfert résumé pour les listes d'activité récente. */
    public record TransfertResume(
            Long id, String nomClient, long montant,
            String statut, String date, String agentNom) {}

    /** Réponse complète du tableau de bord statistiques. */
    public record StatistiquesResponse(
            String portee,                    // "PLATEFORME", "PARTENAIRE" ou "AGENT"
            List<Kpi> kpis,
            List<Part> repartitionOperations, // pour le donut de pourcentage
            List<PointTemporel> evolution,    // pour le graphe de croissance
            List<Classement> classement,      // top agents / partenaires
            List<TransfertResume> activiteRecente) {}
}