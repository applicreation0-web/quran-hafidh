# Ibn Kathīr / Quran.com — accès réel depuis la Carte (10 octobre 2026)

**Modification fonctionnelle effectivement implémentée :**

- Le bouton existant **« Référence »** ouvre toujours la fiche du groupe. **« Lire Ibn Kathīr »** lance désormais la **page Quran.com de son premier verset**, `https://quran.com/en/{sourate}:{verset}/tafsirs/en-tafisr-ibn-kathir`. Le lien est construit depuis les 1 903 groupes indexés et non depuis des paramètres externes. Chaque groupe dispose donc du **commentaire original anglais consultable en un geste**, sans recopier le texte anglais complet dans l'APK.
- Pour les huit groupes de **Qāf (50:1–45)**, la fiche affiche une **petite sélection de titres anglais originaux**, reliés aux huit plages certifiées. Notamment 50:12–15 et 50:16–22 comportent chacun deux titres indépendants **au sein d'un seul groupe documentaire**. Ils ne sont ni resegmentés ni transformés automatiquement en amorces.
- Les 1 895 autres groupes ont une fiche sans titre fabriqué : le bouton conduit au Tafsir exact sur Quran.com. La consultation utilise un navigateur externe et exige une connexion disponible.
- Aucun nouvel icône, bouton de barre d'outils ou moteur ; aucun changement de `SemanticPassageRepository` (Al-Munīr est toujours actif dans Lecture/Révision active/Itqān), ni du Quiz, ni de la progression, ni du Mushaf.
- Les règles de mémorisation ne sont pas déduites du Tafsir : **une amorce reste le commencement d'un tronçon à réciter de mémoire**, pas une phrase qui « évoque » simplement un thème.
- Reste la vérification sur un vrai BOOX, puis la revue exhaustive des clés avant toute substitution d'Al-Munīr.

**Tests** : JUnit `IbnKathirQuranComSourceTest`, vérification des liens pour les 1 903 groupes, validation des 8 plages Qāf, rejet des références aberrantes, contrat source J4, tests JVM Hifz/Quiz et garde-fous CI. Aucun APK produit.

**Référence originale et date :** `spa5k/tafsir_api` source commit `eb82bb6294efe30ad5c135c03b1864afaa70e855`, correspondant aux mêmes groupes visibles sur Quran.com.
