package com.afb.transferplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Compteurs du bilan journalier par agent et par jour. */
@Entity
@Table(name = "compteurs_journaliers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"agent_id", "jour"}))
public class CompteurJournalier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(nullable = false)
    private LocalDate jour;

    private int executes;
    private int rejetes;
    private int annules;
    private int nonClotures;

    public CompteurJournalier() {}

    public CompteurJournalier(Agent agent, LocalDate jour) {
        this.agent = agent;
        this.jour = jour;
    }

    public Long getId() { return id; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
    public LocalDate getJour() { return jour; }
    public void setJour(LocalDate jour) { this.jour = jour; }
    public int getExecutes() { return executes; }
    public void setExecutes(int executes) { this.executes = executes; }
    public int getRejetes() { return rejetes; }
    public void setRejetes(int rejetes) { this.rejetes = rejetes; }
    public int getAnnules() { return annules; }
    public void setAnnules(int annules) { this.annules = annules; }
    public int getNonClotures() { return nonClotures; }
    public void setNonClotures(int nonClotures) { this.nonClotures = nonClotures; }
}