package com.afb.transferplatform.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Gabarits HTML des emails d'invitation — mise en page "banque internationale" :
 * tableaux imbriqués + styles en ligne pour rester compatibles avec Gmail, Outlook,
 * Yahoo Mail, Apple Mail et les clients mobiles.
 *
 * Deux entrées publiques (administrateur de partenaire / agent), qui partagent la
 * même charpente visuelle et ne diffèrent que par le contenu de la carte d'informations.
 */
final class EmailTemplates {

    private EmailTemplates() {}

    private static final DateTimeFormatter FMT_EXPIRATION =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm", Locale.ENGLISH);

    record Champ(String libelle, String valeur, boolean accent) {}

    static String invitationPartenaireAdmin(String urlFrontend, String lienActivation,
                                             java.time.Instant expiration,
                                             String partenaireNom, String administrateurNom,
                                             String email, String motDePasseTemporaire) {
        String expirationTexte = formater(expiration);
        List<Champ> champs = List.of(
                new Champ("Partner Name", partenaireNom, false),
                new Champ("Administrator Name", administrateurNom, false),
                new Champ("Email Address", email, false),
                new Champ("Temporary Password", motDePasseTemporaire, true),
                new Champ("Role", "Partner Administrator", false),
                new Champ("Invitation Expires", expirationTexte, false)
        );
        String intro = "An administrator account has been created for you to manage "
                + "<strong>" + partenaireNom + "</strong>'s activity on the International "
                + "Transfer Monitoring Platform.";
        return enveloppe(urlFrontend, lienActivation, expirationTexte, champs, intro);
    }

    static String invitationAgent(String urlFrontend, String lienActivation,
                                   java.time.Instant expiration,
                                   String agentNom, String partenaireNom, String administrateurNom,
                                   String email, String motDePasseTemporaire) {
        String expirationTexte = formater(expiration);
        List<Champ> champs = List.of(
                new Champ("Agent Name", agentNom, false),
                new Champ("Organization", partenaireNom, false),
                new Champ("Administrator Name", administrateurNom, false),
                new Champ("Email Address", email, false),
                new Champ("Temporary Password", motDePasseTemporaire, true),
                new Champ("Role", "Agent", false),
                new Champ("Invitation Expires", expirationTexte, false)
        );
        String intro = "An agent account has been created for you on the International "
                + "Transfer Monitoring Platform, giving you access to perform and monitor "
                + "international transfers on behalf of <strong>" + partenaireNom + "</strong>.";
        return enveloppe(urlFrontend, lienActivation, expirationTexte, champs, intro);
    }

    private static String formater(java.time.Instant instant) {
        return FMT_EXPIRATION.format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    // ------------------------------------------------------------------

    private static String enveloppe(String urlFrontend, String lienActivation, String expirationTexte,
                                     List<Champ> champs, String introHtml) {

        String logo = urlFrontend + "/assets/images/AFB.png";
        StringBuilder cartes = new StringBuilder();
        for (Champ c : champs) {
            cartes.append(ligneChamp(c.libelle(), c.valeur(), c.accent()));
        }

        return """
        <!doctype html>
        <html lang="en" xmlns="http://www.w3.org/1999/xhtml">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <title>Afriland First Bank — Account Invitation</title>
        <!--[if mso]>
        <noscript>
        <xml><o:OfficeDocumentSettings><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml>
        </noscript>
        <![endif]-->
        <style>
          body, table, td { font-family: 'Inter', Arial, Helvetica, sans-serif; }
          body { margin:0; padding:0; background-color:#F5F7FA; -webkit-text-size-adjust:100%%; }
          img { border:0; outline:none; text-decoration:none; }
          a { text-decoration:none; }
          @media screen and (max-width: 600px) {
            .conteneur { width:100%% !important; }
            .rembourrage { padding-left:20px !important; padding-right:20px !important; }
            .titre-accueil { font-size:22px !important; }
            .bouton-lien { width:100%% !important; display:block !important; }
          }
        </style>
        </head>
        <body style="margin:0;padding:0;background-color:#F5F7FA;">
        <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
          Your Afriland First Bank account is ready — activate it to get started.
        </div>

        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F7FA;">
          <tr>
            <td align="center" style="padding:32px 16px;">

              <table role="presentation" class="conteneur" width="600" cellpadding="0" cellspacing="0"
                     style="width:600px;max-width:600px;background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(23,32,49,0.08);">

                <!-- Bandeau supérieur -->
                <tr>
                  <td style="background-color:#C8102E;height:6px;line-height:6px;font-size:0;">&nbsp;</td>
                </tr>

                <!-- Logo -->
                <tr>
                  <td align="center" class="rembourrage" style="padding:40px 40px 8px;">
                    <img src="%s" width="150" alt="Afriland First Bank" style="display:block;max-width:150px;height:auto;">
                  </td>
                </tr>

                <!-- Nom de la plateforme -->
                <tr>
                  <td align="center" style="padding:0 40px 28px;">
                    <span style="display:inline-block;font-size:11px;font-weight:700;letter-spacing:1.6px;color:#8A93A3;text-transform:uppercase;">
                      International Transfer Monitoring Platform
                    </span>
                  </td>
                </tr>

                <!-- Titre de bienvenue -->
                <tr>
                  <td align="center" class="rembourrage" style="padding:0 40px;">
                    <h1 class="titre-accueil" style="margin:0;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:26px;line-height:1.3;font-weight:800;color:#1A1D23;">
                      Welcome to Afriland First Bank
                    </h1>
                  </td>
                </tr>

                <!-- Introduction -->
                <tr>
                  <td align="center" class="rembourrage" style="padding:16px 48px 32px;">
                    <p style="margin:0;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:14.5px;line-height:1.65;color:#5A6270;">
                      %s
                    </p>
                  </td>
                </tr>

                <!-- Carte d'informations -->
                <tr>
                  <td class="rembourrage" style="padding:0 40px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#F8F9FB;border:1px solid #E7EAF0;border-radius:14px;">
                      <tr>
                        <td style="padding:8px 26px;">
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                            %s
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Bouton d'activation -->
                <tr>
                  <td align="center" class="rembourrage" style="padding:36px 40px 8px;">
                    <table role="presentation" cellpadding="0" cellspacing="0">
                      <tr>
                        <td align="center" bgcolor="#C8102E" style="border-radius:10px;">
                          <!--[if mso]>
                          <v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word"
                                       href="%s" style="height:52px;v-text-anchor:middle;width:280px;" arcsize="16%%" fillcolor="#C8102E" stroke="f">
                          <center style="color:#ffffff;font-family:Arial,sans-serif;font-size:15px;font-weight:bold;">Activate My Account</center>
                          </v:roundrect>
                          <![endif]-->
                          <!--[if !mso]><!-->
                          <a class="bouton-lien" href="%s" target="_blank"
                             style="display:inline-block;padding:16px 48px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:15px;font-weight:700;color:#FFFFFF;background-color:#C8102E;border-radius:10px;letter-spacing:0.2px;">
                            Activate My Account
                          </a>
                          <!--<![endif]-->
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Boîte d'information -->
                <tr>
                  <td class="rembourrage" style="padding:28px 40px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#FDF2F2;border-left:3px solid #C8102E;border-radius:8px;">
                      <tr>
                        <td style="padding:16px 20px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:13px;line-height:1.7;color:#5A6270;">
                          This activation link expires on <strong style="color:#1A1D23;">%s</strong>.<br>
                          During your first login you will be asked to choose a new password.<br>
                          Your temporary password will become invalid immediately after activation.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Fonctionnalités disponibles après activation -->
                <tr>
                  <td class="rembourrage" style="padding:32px 40px 8px;">
                    <p style="margin:0 0 14px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:13px;font-weight:700;color:#1A1D23;">
                      After activation you will be able to:
                    </p>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                      %s
                    </table>
                  </td>
                </tr>

                <!-- Avis de sécurité -->
                <tr>
                  <td class="rembourrage" style="padding:32px 40px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#F8F9FB;border:1px solid #E7EAF0;border-radius:10px;">
                      <tr>
                        <td style="padding:18px 22px;">
                          <p style="margin:0 0 8px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:12.5px;font-weight:700;letter-spacing:0.4px;color:#374151;text-transform:uppercase;">
                            🔒 Security Notice
                          </p>
                          <p style="margin:0;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:12.5px;line-height:1.7;color:#5A6270;">
                            Never share your temporary password. Afriland First Bank will never ask for your
                            password by email or phone. If you did not expect this invitation, please ignore
                            this email or contact Afriland First Bank immediately.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Pied de page -->
                <tr>
                  <td align="center" style="padding:36px 40px 40px;">
                    <p style="margin:0 0 4px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:13px;font-weight:700;color:#1A1D23;">
                      Afriland First Bank
                    </p>
                    <p style="margin:0 0 14px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:11.5px;color:#8A93A3;">
                      International Transfer Monitoring Platform
                    </p>
                    <p style="margin:0 0 14px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:11.5px;color:#8A93A3;">
                      <a href="%s" style="color:#C8102E;font-weight:600;">Official Website</a>
                      &nbsp;•&nbsp;
                      <a href="mailto:contact@afriland.cm" style="color:#C8102E;font-weight:600;">Support Email</a>
                    </p>
                    <p style="margin:0;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:11px;color:#B0B6C0;">
                      © %s Afriland First Bank. All rights reserved.
                    </p>
                  </td>
                </tr>

              </table>
            </td>
          </tr>
        </table>
        </body>
        </html>
        """.formatted(
                logo,
                introHtml,
                cartes.toString(),
                lienActivation,
                lienActivation,
                expirationTexte,
                fonctionnalites(),
                urlFrontend,
                java.time.Year.now()
        );
    }

    private static String ligneChamp(String libelle, String valeur, boolean accent) {
        String valeurStyle = accent
                ? "font-family:'Inter',Arial,Helvetica,sans-serif;font-size:15px;font-weight:700;color:#C8102E;letter-spacing:0.4px;padding:2px 0 15px;"
                : "font-family:'Inter',Arial,Helvetica,sans-serif;font-size:14px;font-weight:600;color:#1A1D23;padding:2px 0 15px;";
        return """
                <tr>
                  <td style="padding:15px 0 0;border-top:1px solid #E7EAF0;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:11px;font-weight:700;letter-spacing:0.5px;color:#8A93A3;text-transform:uppercase;">
                    %s
                  </td>
                </tr>
                <tr>
                  <td style="%s">
                    %s
                  </td>
                </tr>
                """.formatted(libelle, valeurStyle, valeur);
    }

    private static String fonctionnalites() {
        String[] items = {
                "Access your dashboard",
                "Create and manage agents",
                "Perform international transfers",
                "View statistics",
                "Monitor your organization's activities",
                "Manage your profile"
        };
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("""
                    <tr>
                      <td width="24" valign="top" style="padding:5px 0;">
                        <span style="display:inline-block;width:18px;height:18px;line-height:18px;text-align:center;background-color:#E9F7EF;color:#128C4A;border-radius:50%%;font-size:11px;font-weight:700;">✔</span>
                      </td>
                      <td valign="top" style="padding:5px 0 5px 10px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:13.5px;color:#374151;">
                        %s
                      </td>
                    </tr>
                    """.formatted(item));
        }
        return sb.toString();
    }
}
