# Audit contradictoire — Quran Safeguard 0.10.0

> Document historique. Pour le comportement et les garanties actuels, consulter `ADVERSARIAL_AUDIT_0.10.1.md`, qui remplace notamment l'ancienne hypothèse d'une souscription Android toujours étroite.

Date : 2026-09-03  
Base auditée : branche `fix/0.9.2-roadmap-final-audit`, après intégration des points 1 à 4 et des corrections contradictoires.

## Méthode

L’audit cherche volontairement à invalider l’application sur sept axes : périmètre Android, exactitude du temps cumulé, cycle 15/90 minutes, lecture 60 secondes, limites Juz/Hizb, transitions ergonomiques et reprise après interruption.

## Résultats et corrections

| Risque attaqué | Constat contradictoire | Correction vérifiée |
| --- | --- | --- |
| Application bancaire perturbée | Un abonnement d’accessibilité global peut faire réagir une application sensible même sans blocage explicite. | Le service ne définit jamais `packageNames = null`. Sa liste reste limitée aux 14 cibles, à Safeguard, à System UI et au lanceur courant. Aucun identifiant bancaire, professionnel, GPS, transport ou santé n’est découvert, classé, stocké ou reçu dans les événements. |
| Sortie du périmètre | Une liste d’exclusions imposerait de connaître toutes les applications du téléphone. | La liste d’exclusions et les classifieurs ont été supprimés. Seules les cibles choisies déclenchent une restriction. |
| Chrome puis YouTube | Deux crédits ou une remise à zéro au changement d’application permettraient de dépasser 15 minutes. | Toutes les cibles utilisent la même clé de budget global. L’ancienne cible est arrêtée avant le démarrage de la nouvelle. Les tests 8+7 minutes, 4+6+5 minutes et checkpoints fréquents atteignent tous exactement zéro sans recrédit. |
| Expiration interrompue | Un arrêt entre zéro et la création du palier pourrait affaiblir le sixième intervalle. | Le niveau en attente est persisté avant invalidation du crédit et l’accès exige l’absence de palier. La reconstruction conserve MICRO contre HIZB. |
| Validation ou joker interrompu | Un arrêt pourrait accorder un crédit sans terminer le palier, ou consommer un joker sans crédit. | Niveau, crédit de 15 minutes, consommation du joker et historique sont validés dans des transactions cohérentes. |
| Minuteur de page bloqué | Le bouton pouvait rester inutilisable après 60 secondes si la détection de défilement n’avait pas reçu le signal attendu. | La fin de page utilise aussi `canScrollVertically(1)`. Le statut indique clairement « 01:00 atteint » et le balayage devient disponible. |
| Navigation peu intuitive | Un bouton unique ne permettait pas de revoir naturellement les pages lues. | Balayage horizontal animé, retour libre vers toute page validée et avance impossible au-delà de la page active avant 60 secondes et fin de page. Les boutons précédent/suivant restent accessibles. |
| Quota terminé | La fermeture automatique empêchait de poursuivre une lecture engagée. | À la dernière page obligatoire, le déblocage est acquis sans fermer le lecteur. L’utilisateur choisit « Ouvrir l’application cible » ou « Continuer à lire » librement, sans nouveau minuteur. |
| Limites Hizb/Juz | Les anciennes plages supposaient des blocs de pages réguliers, alors que les divisions commencent ou finissent souvent au milieu d’une page. | Les 30 Juz et 60 Hizb suivent leurs versets exacts et la pagination Médine 604 pages. Une page frontière peut appartenir aux deux sections adjacentes ; l’interface affiche versets, pages et changement de section. |
| Quota de 10 pages | Un Hizb physique peut occuper 9, 10, 11 pages ou davantage. | La vérité structurelle et la règle produit sont séparées : le lecteur montre les limites réelles, mais le palier exige toujours dix pages consécutives. Si la limite réelle est franchie, le changement de Hizb est affiché. |
| Commentaires Hikam | Une interprétation ajoutée pouvait être confondue avec la parole originale. | Le modèle, l’écran détail et la bibliothèque ne conservent plus que la Hikma arabe vocalisée, la traduction française et la source. |
| Retrait d’une cible | Un décochage immédiat permettrait de contourner le cycle en cours. | L’ajout et l’annulation sont immédiats ; un retrait reste actif aujourd’hui et devient effectif au prochain jour local. Désinstallation : retrait immédiat. Six tests couvrent la règle. |
| Appels WhatsApp | Texte et appel utilisent le même paquet Android. | Les modes audio de téléphonie/VoIP et les activités d’appel WhatsApp connues suspendent le budget. Aucune permission de journal d’appels, téléphone ou notifications n’est ajoutée. |
| Retour des réglages Android | L’activation pouvait sembler figée après la page système. | La reprise revérifie le service, confirme l’activation puis retourne automatiquement à l’application ; les initialisations OEM non essentielles ne peuvent plus arrêter tout le service. |

Les limites exactes sont fondées sur [Tanzil Quran Metadata](https://tanzil.net/docs/quran_metadata) et présentées selon le Mushaf de Médine 604 pages, conformément au principe de pagination documenté par [Quran Foundation](https://api-docs.quran.foundation/docs/tutorials/fonts/page-layout/).

## Vérifications automatisées

- Audit Gradle total : périmètre strict, confidentialité, 264 Hikam, 604 pages, migrations, timer global et règles éditoriales.
- Tests unitaires : cycle 15/90, budget partagé multi-cibles, checkpoints, reprise, appels, règle du lendemain, métadonnées Juz/Hizb et quota de dix pages.
- Compilation Kotlin, APK debug et APK release réussies.
- APK de diffusion : un signataire, schémas v2 et v3 valides, archive ZIP intègre.
- Certificat historique SHA-256 : `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC`.
- APK signée : 71 976 853 octets, SHA-256 `32ad6777fc6a6d6b872ba63b7cbdf5c5530211dc96004405aa32716e93992b2d`.

## Limites honnêtes à vérifier sur téléphone

1. Android et ses fabricants peuvent retarder certains événements d’accessibilité ; le ticker réagit toutes les 250 ms et persiste un checkpoint chaque seconde, mais une campagne réelle Chrome → YouTube reste utile.
2. Une application bancaire peut décider par sa propre politique de refuser tout téléphone ayant un service d’accessibilité activé. Safeguard ne reçoit plus ses événements et ne peut pas neutraliser une politique interne de la banque ; ce cas ne peut être tranché que sur le téléphone concerné.
3. WhatsApp peut renommer une activité d’appel ; `AudioManager` reste donc le mécanisme principal.
4. Soixante secondes attestent une présence active sur la page, pas la compréhension spirituelle.
5. Un identifiant régional ou « Lite » inconnu reste hors périmètre jusqu’à son ajout explicite.

## Verdict

Aucun contournement logiciel certain n’est resté ouvert dans le modèle testé. Le périmètre est strictement positif, le quart d’heure est commun à toutes les cibles, la lecture respecte les limites coraniques exactes sans interrompre l’élan après quota, et le paquet final conserve la lignée de signature historique.
