package com.afb.transferplatform.utile;

import java.security.SecureRandom;

/** Génère les mots de passe temporaires envoyés lors d'une invitation. */
public final class MotDePasseGenerator {

    private static final String MAJUSCULES = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULES = "abcdefghijkmnopqrstuvwxyz";
    private static final String CHIFFRES   = "23456789";
    private static final String SPECIAUX   = "@#$%&*";
    private static final String TOUS = MAJUSCULES + MINUSCULES + CHIFFRES + SPECIAUX;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LONGUEUR = 10;

    private MotDePasseGenerator() {}

    /** Format : Afb@XXXXXX — préfixe reconnaissable + 6 caractères aléatoires. */
    public static String generer() {
        StringBuilder sb = new StringBuilder("Afb@");
        sb.append(MAJUSCULES.charAt(RANDOM.nextInt(MAJUSCULES.length())));
        sb.append(CHIFFRES.charAt(RANDOM.nextInt(CHIFFRES.length())));
        for (int i = sb.length(); i < LONGUEUR; i++) {
            sb.append(TOUS.charAt(RANDOM.nextInt(TOUS.length())));
        }
        return sb.toString();
    }
}