# Quran Safeguard

Quran Safeguard est une application Android privée de discipline numérique : elle place une pause de lecture du Coran avant un périmètre volontairement limité d’applications.

## Règles de protection 0.10.1

- Cibles sociales : WhatsApp (messagerie textuelle), X, Instagram, Facebook, YouTube et TikTok.
- Navigateurs : Chrome, Firefox, Edge, Brave, Opera, Samsung Internet, DuckDuckGo et Vivaldi.
- Toutes les autres applications restent hors périmètre, sans liste d’exclusion à configurer ni classification locale.
- Les appels téléphoniques et les appels audio/vidéo WhatsApp ne consomment jamais le temps protégé.
- Premier accès quotidien : 20 pages du Mushaf, au moins 60 secondes actives par page.
- Ensuite : une page au moins 60 secondes après chaque cumul global de 15 minutes de présence effective au premier plan dans les cibles.
- À chaque sixième cumul, soit exactement 90 minutes de présence effective dans les cibles : un bloc de 10 pages remplace la page simple puis le cycle repart à zéro.
- Trois jokers quotidiens peuvent franchir n’importe lequel de ces paliers ; chaque joker ouvre le prochain intervalle normal de 15 minutes.
- Le crédit de 15 minutes est unique et partagé entre toutes les cibles : passer de Chrome à YouTube, puis à une autre cible, ne remet jamais le chronomètre à zéro.
- Les rappels d’usage à 10, 5 et 1 minute restent bienveillants et distincts des rappels spirituels.

## Lecture

- Mushaf de Médine Hafs ‘an ‘Asim, 604 pages canoniques, disponible hors ligne.
- Mise en page originale de 15 lignes.
- Une page ne peut être validée qu’après 60 secondes actives. Le défilement reste libre et ne peut plus laisser la page bloquée au-delà de la minute.
- Le temps de lecture s’arrête en arrière-plan, écran éteint ou en multi-fenêtre lorsque le lecteur n’est plus l’activité principale.
- Les débuts et fins de Juz/Hizb suivent leurs versets exacts dans la pagination du Mushaf de Médine : une page frontière peut donc appartenir à deux sections adjacentes.
- Le quota produit demandé reste de 10 pages par bloc, même lorsqu’un Hizb réel occupe 9, 11 pages ou davantage ; l’interface signale clairement le changement de section.
- Si un seul Hizb est choisi, son bloc de 10 pages est répété pour former les 20 pages matinales.
- Avec plusieurs Hizb, la progression quotidienne est séquentielle du plus petit numéro au plus grand.
- Après la dernière page obligatoire, le déblocage est acquis immédiatement ; l’utilisateur peut ouvrir la cible ou continuer librement les pages suivantes, sans nouveau minuteur.

## Confidentialité et fluidité

- L’AccessibilityService reste sur un périmètre fixe lorsque le compteur est arrêté : Quran Safeguard, les cibles sélectionnées, Android System UI et le lanceur courant.
- Pendant qu’une cible consomme réellement le compteur, le service accepte le premier événement extérieur comme simple signal anonyme de sortie, arrête le chrono, puis revient immédiatement au périmètre fixe.
- Aucune application bancaire, professionnelle, GPS, transport, santé, identité ou sécurité n’est classifiée, journalisée ou associée à Safeguard.
- Aucune permission de journal d’appels, d’état téléphonique ou d’écoute des notifications n’est demandée.
- La récupération du contenu des fenêtres d’accessibilité reste désactivée.
- La désinstallation Android normale reste possible.

## Rappels spirituels

- Une pensée principale est choisie localement pour la journée et reste stable.
- Notification quotidienne douce à 20:00 locale.
- Les hadiths automatiquement retenus sont limités à Sahih al-Bukhari et Sahih Muslim ; les autres textes conservés indiquent leur degré et leur provenance.
- Quran Safeguard n’interprète ni ne reconstruit la voix d’Ibn ʿAṭāʾ Allāh, d’Ibn ʿAjība ou d’al-Ghazālī.
- Les horaires d’adhkar sont calculés localement ; les coordonnées ne sont pas téléversées.

## Mise à jour et signature

- L’identifiant Android reste `com.applicreation0.quransafeguard`.
- Les migrations sont sauvegardées, cumulatives et réversibles en cas d’échec.
- La version 0.10.0 a migré les anciens crédits par application et les anciennes exclusions vers le compteur global protégé ; la 0.10.1 conserve ce schéma et verrouille le cumul de présence cible.
- Les versions de test doivent conserver la lignée de signature historique `6C:70:6F:4E:…:AC`.
- La clé PKCS12 et ses mots de passe ne doivent jamais être ajoutés au dépôt.

Le paquet de diffusion n’est publié qu’après réussite de l’audit, des tests unitaires, des compilations debug/release et de la vérification cryptographique de sa signature.
