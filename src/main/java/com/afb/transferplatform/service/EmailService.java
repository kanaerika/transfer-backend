package com.afb.transferplatform.service;

/**
 * Abstraction d'envoi d'email. Le corps est toujours du HTML.
 * Implémentation dev : ConsoleEmailService (logs, aplati en texte lisible).
 */
public interface EmailService {
    void envoyer(String destinataire, String sujet, String messageHtml);
}
