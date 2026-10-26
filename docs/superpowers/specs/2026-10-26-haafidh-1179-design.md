# Haafidh 1.17.9 — design et inventaire différentiel

Mandat : cahier des charges utilisateur du 26 octobre 2026, 15 sections. Exécution autorisée jusqu'à candidate debug et rapport ; aucune publication officielle.

Base technique : ae68ee1a46e195e9da1068aed981f3363869a4e0, branche 1.17.8 ; code applicatif audité par CI 40405581372. Candidate signée 1.17.8/code46 existante mais validation physique ouverte. Référence officielle antérieure documentée : c42bc5de (1.17.7/code45). Le marqueur GitHub latest pointe encore sur 1.17.1 : il ne prouve pas l'état du paquet installé.

## Inventaire à préserver

MainActivity : paramètres ; action Aujourd'hui avec liste et détails ; rattrapages FIFO ; avis Sabqi ; quatre destinations Parcours/Progression/Lecture/Atlas ; six modes Hifz ; état Maintien après Nās ; programme hebdomadaire avec confirmation ; sept dates ; détails complets matin/soir ; bilan contrôles/retards ; récap lectures cumulatives ; reprises de finales datées, Renforcement ouvert et lecture cumulative. Toutes les destinations et gardes métier restent en place.

## Architecture retenue

Refonte native Android Views, sans dépendance nouvelle. MainActivity conserve ses actions ; nouveau modèle CalendarPresentation pour décomposer les activités réelles d'une ligne de planning et calculer les états d'affichage. WeeklyDashboardPlanner garde ses règles ; surcharge de fenêtre avec aujourd'hui réel distinct de la première date consultée. Historique : reçus seulement, absence de preuve explicitement inconnue ; jamais de plages anciennes inventées.

Accueil : identité/date, priorité existante, quatre accès compacts, frise sept jours + compteurs, reprises historiques datées. Parcours : trois paires de tuiles compactes, calendrier immédiat, semaine précédente/suivante, Aujourd'hui, mois, historique, configuration séparée. Cartes : titre/état propres, détail original intégral développé au toucher, accès via les gardes existantes uniquement.

## Approches

1. Tout réécrire en Compose : écartée, introduit dépendances et risque de reprise.
2. Nouveau planificateur UI : écarté, dupliquerait les règles de Hifz.
3. Adaptateur d'affichage + Views existantes : retenu conformément au mandat de présentation seule.

Blanc/noir, contour explicite, cibles >=48dp, taille des icônes contractuelles réduite sans modifier leurs fichiers, aucune animation continue. Dimensions Android issues de Configuration et insets Ui.respectSystemBars ; aucune conversion de photographie en dp.

## Invariants et validation

Aucune migration, nouvelle préférence ni modification des moteurs/acquis/initialisations/signatures historiques. La navigation et l'expansion des cartes n'écrivent pas d'état Hifz. État de navigation restauré dans Bundle. Toutes informations non disponibles signalées comme telles. Prévisions futures distinguées des reçus.

Tests : décomposition 2+2, zéro activité, états partiels/inconnus, finales, frontières mois/année, préservation des reçus, navigation pure ; suites existantes et simulations 4/8 semaines et 180/365 jours ; Android portrait compact/tablette si CI disponible. Comparaison des moteurs par hash avant/après. Captures natives uniquement comme preuves finales. Signature officielle et BOOX physique : portes distinctes, jamais déclarées PASS sans exécution.
