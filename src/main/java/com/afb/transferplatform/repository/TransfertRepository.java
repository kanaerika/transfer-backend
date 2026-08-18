package com.afb.transferplatform.repository;
 
 
import com.afb.transferplatform.entity.StatutTransfert;
import com.afb.transferplatform.entity.Transfert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
public interface TransfertRepository extends JpaRepository<Transfert, Long> {
 
    List<Transfert> findAllByOrderByIdDesc();
 
    List<Transfert> findByStatutOrderByIdDesc(StatutTransfert statut);
 
    Optional<Transfert> findFirstByNomClientIgnoreCaseOrderByIdDesc(String nomClient);
    /** Transferts d'un agent pour un jour donné (tous statuts), pour le bilan journalier. */
    List<Transfert> findByAgentIdAndDateTransfertOrderByIdDesc(Long agentId, LocalDate jour);
 
    /**
     * Cumul Hors CEMAC du mois : somme des transferts EXECUTES du client sur la période.
     * L'identité du client est vérifiée par nom + n° de pièce + date de naissance (pas le nom seul),
     * pour éviter qu'un homonyme soit confondu avec un autre client.
     */
    @Query("""
           SELECT COALESCE(SUM(t.montant), 0)
           FROM Transfert t
           WHERE UPPER(t.nomClient) = UPPER(:nomClient)
             AND UPPER(t.numeroPiece) = UPPER(:numeroPiece)
             AND t.dateNaissance = :dateNaissance
             AND t.statut = com.afb.transferplatform.entity.StatutTransfert.EXECUTE
             AND t.paysDestination NOT IN ('Cameroun', 'Congo', 'Gabon',
                 'Guinée équatoriale', 'République centrafricaine', 'Tchad')
             AND t.dateTransfert BETWEEN :debut AND :fin
           """)
    long cumulMensuel(@Param("nomClient") String nomClient,
                      @Param("numeroPiece") String numeroPiece,
                      @Param("dateNaissance") String dateNaissance,
                      @Param("debut") LocalDate debut,
                      @Param("fin") LocalDate fin);
 
    /** Clients connus (infos du transfert le plus récent de chaque client) dont le nom contient la saisie. */
    @Query("""
           SELECT t FROM Transfert t
           WHERE UPPER(t.nomClient) LIKE CONCAT('%', UPPER(:recherche), '%')
             AND t.id = (SELECT MAX(t2.id) FROM Transfert t2 WHERE UPPER(t2.nomClient) = UPPER(t.nomClient))
           ORDER BY t.nomClient
           """)
    List<Transfert> rechercherClientsConnus(@Param("recherche") String recherche);

    /** Historique scopé au partenaire de l'utilisateur connecté. */
    List<Transfert> findByPartenaireIdOrderByIdDesc(Long partenaireId);

    List<Transfert> findByPartenaireIdAndStatutOrderByIdDesc(Long partenaireId, StatutTransfert statut);

    // ---- Statistiques ----

    long countByPartenaireId(Long partenaireId);
    long countByPartenaireIdAndStatut(Long partenaireId, StatutTransfert statut);
    long countByAgentId(Long agentId);
    long countByAgentIdAndStatut(Long agentId, StatutTransfert statut);
    long countByStatut(StatutTransfert statut);

    long countByDateTransfertBetween(LocalDate debut, LocalDate fin);
    long countByPartenaireIdAndDateTransfertBetween(Long partenaireId, LocalDate debut, LocalDate fin);
    long countByAgentIdAndDateTransfertBetween(Long agentId, LocalDate debut, LocalDate fin);

    /** Répartition par statut sur toute la plateforme. */
    @Query("SELECT t.statut, COUNT(t) FROM Transfert t GROUP BY t.statut")
    List<Object[]> repartitionParStatut();

    @Query("SELECT t.statut, COUNT(t) FROM Transfert t WHERE t.partenaire.id = :pid GROUP BY t.statut")
    List<Object[]> repartitionParStatutPartenaire(@Param("pid") Long partenaireId);

    @Query("SELECT t.statut, COUNT(t) FROM Transfert t WHERE t.agent.id = :aid GROUP BY t.statut")
    List<Object[]> repartitionParStatutAgent(@Param("aid") Long agentId);

    /** Nombre de transferts par mois (12 derniers), plateforme entière. */
    @Query("""
           SELECT EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert), COUNT(t)
           FROM Transfert t
           WHERE t.dateTransfert >= :depuis
           GROUP BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           ORDER BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           """)
    List<Object[]> transfertsParMois(@Param("depuis") LocalDate depuis);

    @Query("""
           SELECT EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert), COUNT(t)
           FROM Transfert t
           WHERE t.partenaire.id = :pid AND t.dateTransfert >= :depuis
           GROUP BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           ORDER BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           """)
    List<Object[]> transfertsParMoisPartenaire(@Param("pid") Long partenaireId,
                                               @Param("depuis") LocalDate depuis);

    @Query("""
           SELECT EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert), COUNT(t)
           FROM Transfert t
           WHERE t.agent.id = :aid AND t.dateTransfert >= :depuis
           GROUP BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           ORDER BY EXTRACT(YEAR FROM t.dateTransfert), EXTRACT(MONTH FROM t.dateTransfert)
           """)
    List<Object[]> transfertsParMoisAgent(@Param("aid") Long agentId,
                                          @Param("depuis") LocalDate depuis);

    /** Top agents d'un partenaire : [agentId, nomComplet, nbTransferts]. */
    @Query("""
           SELECT t.agent.id, t.agent.nomComplet, COUNT(t)
           FROM Transfert t
           WHERE t.partenaire.id = :pid AND t.agent IS NOT NULL
           GROUP BY t.agent.id, t.agent.nomComplet
           ORDER BY COUNT(t) DESC
           """)
    List<Object[]> topAgents(@Param("pid") Long partenaireId, Pageable pageable);

    /** Top partenaires plateforme : [partenaireId, nom, nbTransferts]. */
    @Query("""
           SELECT t.partenaire.id, t.partenaire.nom, COUNT(t)
           FROM Transfert t
           GROUP BY t.partenaire.id, t.partenaire.nom
           ORDER BY COUNT(t) DESC
           """)
    List<Object[]> topPartenaires(Pageable pageable);

    /** Derniers transferts d'un partenaire. */
    List<Transfert> findTop8ByPartenaireIdOrderByIdDesc(Long partenaireId);
    List<Transfert> findTop8ByAgentIdOrderByIdDesc(Long agentId);
}
