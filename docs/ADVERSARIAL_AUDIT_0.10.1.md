# Second audit contradictoire — Quran Safeguard 0.10.1

Date : 2026-09-03  
Code fonctionnel audité : `973587f1db5dd79fd5d23aba112002fe6b12f0d5`
Statut : candidat signé, non fusionné et non publié.

## Périmètre attaqué

L'audit a cherché à invalider les invariants de sortie : cumul réel des applications cibles, priorité du palier de 90 minutes, absence de débit hors cible, reprise après appel ou interruption, règle du lendemain, minuterie et navigation du lecteur, données Hizb/Juz, contenu des Hikam, ergonomie et continuité cryptographique.

## Constats contradictoires et corrections

| Attaque | Défaut ou risque observé | Correction retenue et vérifiée |
| --- | --- | --- |
| Chrome puis YouTube puis WhatsApp | Un compteur par application ou une remise à zéro au changement permettrait de dépasser quinze minutes. | Un seul registre persistant additionne exclusivement la présence au premier plan de toutes les cibles sélectionnées. Le changement de cible conserve le reliquat. Le même registre forme les six quarts d'heure du cycle de 90 minutes. |
| Application hors cible pendant un crédit actif | Une souscription Android toujours limitée aux seuls paquets cibles ne signale pas nécessairement la sortie vers une application arbitraire ; le temps pourrait continuer à être débité. | Pendant qu'une cible détient réellement le budget, le service ouvre un signal de sortie anonyme à usage unique. Le premier événement extérieur arrête le débit immédiatement puis rétablit la liste étroite. Aucun paquet extérieur n'obtient le budget, n'est classé, affiché, journalisé ou persisté. |
| Application bancaire | Certaines banques refusent tout appareil ayant un service d'accessibilité tiers activé, même si elles ne sont ni ciblées ni observées durablement. | Aucune banque n'est ajoutée au périmètre et aucune logique de blocage hors cible n'existe. La politique interne d'une banque reste toutefois impossible à neutraliser en conservant l'architecture AccessibilityService. Le contournement opérationnel accepté est l'autre appareil ou la désactivation temporaire de Safeguard. |
| Appel puis fin d'appel | La fin d'un appel pouvait reprendre une ancienne cible sans nouvel événement fiable. | La fin d'appel ne relance plus un paquet mémorisé. Le débit ne repart qu'au prochain événement réel d'une cible sélectionnée. L'état d'appel WhatsApp est remis à zéro avec l'état audio. |
| Cible décochée encore ouverte au changement de jour | Sans nouvel événement de fenêtre, le retrait différé pouvait continuer à débiter après minuit. | Le contrôle périodique de 250 ms relit la sélection effective et arrête le budget dès que la règle du lendemain rend le retrait actif. |
| Sixième quart d'heure | Un micro-palier pouvait concurrencer le palier Hizb de 90 minutes. | Le sixième intervalle produit uniquement le palier Hizb. Sa validation clôt le cycle cumulatif et recrédite le nouveau cycle ; aucun micro-palier supplémentaire n'est empilé. |
| Page affichée depuis plus de 60 secondes | Le lecteur pouvait rester bloqué si le WebView ne remontait jamais un signal de bas de page. | La validation dépend désormais uniquement de 60 secondes de présence active sur la page. Le défilement reste une information ergonomique et ne prolonge jamais le verrou. |
| Balayage du lecteur | Un geste pouvait permettre d'avancer au-delà de la page active ou empêcher de revoir une page déjà lue. | Le retour par balayage reste libre vers les pages validées. L'avance ne devient possible qu'après les 60 secondes de la dernière page atteinte. Après le quota, la sortie est libre et la lecture peut continuer sans nouveau verrou. |
| Retour vers l'application cible | Après la dernière page, le crédit était accordé mais un écran intermédiaire obligeait encore à toucher « Ouvrir l'application cible ». | Le swipe ou le bouton final retire désormais immédiatement la tâche temporaire de Safeguard et révèle l'écran exact de la cible resté dessous, sans toast ni délai. Un lancement par l'icône n'est utilisé qu'en secours si Android a perdu cette tâche. « Valider et continuer à lire » reste un choix explicite sur la dernière page et n'ajoute aucune friction au parcours normal. Les jokers suivent la même transition immédiate. |
| Frontières Hizb/Juz | Une division coranique peut commencer ou finir au milieu d'une page ; des blocs artificiels de dix pages seraient trompeurs. | Les métadonnées présentent les versets et pages exacts des 60 Hizb et 30 Juz dans le Mushaf de Médine 604 pages, y compris les pages frontières partagées. La règle produit des dix pages obligatoires reste distincte de la frontière physique et l'interface signale le passage de section. |
| Hikam | Des commentaires ou une interprétation ajoutée pouvaient être confondus avec le texte retenu. | Les commentaires ont disparu du modèle et de l'interface. La bibliothèque contient 264 Hikam vérifiées : arabe vocalisé, traduction française et source uniquement. |
| Carte de progression | L'API de la version Compose utilisée n'acceptait pas le paramètre de bordure choisi. | La compilation a fait échouer la première tentative ; la bordure est maintenant appliquée par `Modifier.border`, compatible avec la version du projet. |
| Garde d'audit trop littérale | Une assertion de l'audit Gradle échouait à cause d'un retour à la ligne dans le code, pas d'un défaut produit. | La garde vérifie désormais l'invariant sémantique stable. La CI suivante a exécuté l'audit, les tests et les deux constructions APK jusqu'au bout. |

## Stress test de sortie

La suite de 115 tests unitaires comprend quatre scénarios de charge bloquants pour la release et quatre scénarios dédiés au retour vers la cible :

- plus de 1 000 rafales de présence cible, séparées par de longues périodes banque/GPS/travail/appel simulées, épuisent exactement 15 minutes et jamais davantage ;
- 250 cycles complets de 90 minutes, soit 600 000 alternances rapides réparties sur six intervalles par cycle, conservent un total exact de 22 500 minutes cibles simulées ;
- 100 000 décisions de périmètre vérifient qu'une application hors cible ne peut jamais devenir propriétaire du budget ;
- 1 000 000 de valeurs autour de la frontière des 60 secondes vérifient simultanément le reliquat et l'autorisation d'avancer.
- retour normal vers la tâche exacte, lancement de secours, cible désinstallée et paquet vide sont chacun verrouillés par un test de non-régression.

Cela représente plus de 1,7 million d'itérations de charge et plus de 2,8 millions d'assertions déterministes. La CI Android no 534 a réussi l'audit release, les 115 tests, la compilation Kotlin, l'APK debug et l'APK release non signé.

## Vérification du paquet candidat

- APK signée : 71 981 397 octets.
- SHA-256 : `43c3d61e96ee23eb2c289266d99ff6dcb2da7898a92c88aa30dc0283b1c3a1f2`.
- Archive ZIP : intègre ; toutes les entrées non compressées sont alignées sur quatre octets et les quatre bibliothèques natives sur 16 Kio.
- Signature : schémas APK v2 et v3 valides, un seul signataire RSA 4096 bits.
- Certificat historique SHA-256 : `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC`.

## Limites résiduelles honnêtes

1. Le stress test est déterministe et massif, mais il ne remplace pas une campagne instrumentée sur le téléphone final et sa surcouche constructeur.
2. Android ou un fabricant peut retarder un événement d'accessibilité ; le contrôle de 250 ms et les checkpoints limitent l'écart, sans promettre une précision physique à la milliseconde.
3. Une banque peut continuer à refuser la présence de tout service d'accessibilité tiers. Ce comportement n'est pas un blocage déclenché par Safeguard et ne peut pas être corrigé sans changer d'architecture ou de politique côté banque.
4. La détection des appels WhatsApp combine l'état audio et les activités d'appel connues ; un changement futur de WhatsApp devra être retesté sur appareil.
5. L'installation comme mise à jour au-dessus de chaque ancienne APK doit encore être validée sur appareil avant toute diffusion publique, notamment pour l'artefact 0.9.0 signé avec un autre certificat.

## Verdict

Le candidat 0.10.1 respecte le contrat logiciel testé : les 15 et 90 minutes sont du temps cumulé de présence dans les seules applications cibles ; les périodes hors cible et les appels ne sont pas débitées ; le lecteur ne reste plus verrouillé après 60 secondes ; la navigation et les limites coraniques sont cohérentes ; les Hikam ne contiennent plus de commentaires ; le déblocage rend immédiatement l'application déclencheuse sans écran intermédiaire, sauf choix volontaire de poursuivre la lecture. Aucun défaut logiciel bloquant n'est resté ouvert dans les scénarios automatisables. La publication reste volontairement suspendue jusqu'à la validation sur appareil demandée ci-dessus.
