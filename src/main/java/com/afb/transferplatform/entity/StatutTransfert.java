package com.afb.transferplatform.entity;
public enum StatutTransfert {
    EXECUTE("Transfert exécuté"),
    ANNULE("Transfert annulé"),
    REJETE("Transfert rejeté"),
    EN_COURS("En cours");
 
    private final String libelle;
 
    StatutTransfert(String libelle) { this.libelle = libelle; }
 
    public String getLibelle() { return libelle; }
}
 
