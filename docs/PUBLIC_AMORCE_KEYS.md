# Quran Haafidh — clés de localisation des amorces (public)

L’utilisateur autorise désormais à versionner dans GitHub **les clés/repères coraniques des amorces**.

`docs/ibn_kathir_public_amorce_keys_v1.csv` inventorie 1 903 groupes Ibn Kathīr sur les 114 sourates, couvrant les 6 236 versets. Il contient pour chaque groupe l’identifiant immuable, l’intervalle de versets et la première **clé de mot QCF** (`sourate:verset:position`, index 1). La présence ou l’absence d’un titre est seulement signalée comme booléen (1 708/195), sans le reproduire.

**Distinction essentielle :** le début documentaire d’un groupe n’est **pas** automatiquement une « clé de récitation » approuvée sur le plan éditorial. Les propositions de deux premiers mots fournies par `IbnKathirFirstWordsCorpus` suivent la géométrie QCF ; elles ne doivent pas être présentées comme des validations sémantiques humaines. Aucune décision de masquage Hifz n’est déduite de ce CSV.

Ce fichier **ne contient ni les 1 708 titres anglais**, ni des glosses anglaises, ni un corpus provenant de l’API Quran Foundation. Une autorisation explicite de versionner des *clés* ne démontre pas les droits permettant de publier les textes anglais tiers dans un dépôt public. Le texte arabe coranique et ses coordonnées restent ceux du Muṣḥaf existant.

Empreinte SHA-256 du CSV : `865dd26e66cc186f7f9a5ec87c9c065597befa350be5e1e0e55efca8494f7dfb`.
