# Confidant

Confidant est une messagerie privée en développement. L'interface reste dans un seul fichier HTML autonome, partagé entre le site web et la coquille Android.

## Ce qui est implémenté dans la base actuelle

- Identité éditoriale ivoire / olive / bronze ; fichier de travail `confident.html` et copie publiée `site/index.html`.
- Cryptographie Web Crypto : ECDH P-256, HKDF-SHA-256 et AES-256-GCM.
- Création d'invitation avec lien copiable / QR code ; échanges directs PeerJS / WebRTC.
- Plusieurs conversations locales, noms personnalisés, historique séparé par identifiant et tentative de reprise avec les identités et clés ECDH stockées dans IndexedDB.
- File d'attente locale des messages texte ; reprise limitée de la connexion, avec temporisations progressives.
- Accusé de réception texte chiffré : l'émetteur n'affiche « Distribué » que lorsqu'un ACK authentifié revient après le déchiffrement et l'enregistrement du message côté destinataire. Les doublons sont ré-accusés pour permettre une reprise après perte d'ACK.
- Transfert de médias par fragments chiffrés et ACK de fin de transfert ; enregistrement vocal, limites de taille, expiration et stockage local.
- Commandes de contrôle chiffrées pour l'effacement d'historique.
- Coquille Android WebView avec permission microphone explicite, sélecteur de fichiers et navigation ; workflows pour compiler l'APK et publier le site.

## Limites importantes — lire avant usage

**Ce prototype n'a pas été testé en bout-en-bout entre deux appareils et n'a pas été audité.** La syntaxe JavaScript et la compilation Android passent en CI, mais cela ne prouve pas que WebRTC, les reconnexions, les médias ou les accusés se comportent correctement dans tous les réseaux.

- Une seule connexion WebRTC active est gérée à la fois. Plusieurs conversations et leurs historiques peuvent être conservés/sélectionnés ; les autres conversations ne reçoivent pas en arrière-plan.
- Seuls les messages texte sont mis en attente hors connexion. Les médias doivent être renvoyés lorsque la connexion est active.
- PeerJS fournit un service de signalisation tiers. Aucun serveur de stockage central ne conserve le contenu des messages. En revanche, le site et les bibliothèques CDN sont des dépendances externes.
- Aucun serveur TURN n'est configuré ; certains réseaux peuvent bloquer la connexion directe. L'arrivée du destinataire en ligne et la disponibilité du navigateur sont nécessaires à l'établissement de la liaison.
- Les clés ECDH sont conservées dans IndexedDB pour permettre une reprise, mais le stockage local n'est pas chiffré par un PIN. Les médias sauvegardés dans IndexedDB sont des Blob locaux non chiffrés au repos.
- La même paire ECDH peut être réutilisée à travers des reprises : il n'y a pas de Double Ratchet ni de confidentialité persistante complète. Un utilisateur doit comparer l'empreinte via un canal indépendant.
- Un effacement demandé à distance n'efface pas les captures, exports ou copies sauvegardées. Le nettoyage local ne garantit pas la suppression de l'appareil distant.
- Le stockage du navigateur et celui du WebView Android sont distincts ; il n'y a pas de synchronisation entre eux.
- L'APK actuelle est un APK debug non signé avec une clé de publication stable. Certaines mises à jour peuvent demander une désinstallation.

## Publication web

Le workflow `Publier Confidant sur GitHub Pages` déploie `site/` quand Pages est activé. Une activation manuelle est requise dans GitHub : **Settings → Pages → Build and deployment → Source → GitHub Actions**. L'intégration GitHub connectée ne dispose pas de la permission nécessaire pour créer/activer le site à elle seule.

URL prévue : https://mediagrabofficial.github.io/confidant/

Sans activation de Pages, l'APK compile mais ne peut pas charger l'interface distante à cette URL. Après activation, vérifiez qu'un workflow Pages se termine par `success`.

## Télécharger l'APK

Chaque push lance le workflow **Compiler l'APK Confidant**. Une fois terminé, téléchargez l'artefact `Confidant-debug-apk` depuis l'onglet **Actions**. L'APK ouvre le site publié ; il ne contient pas une copie autonome du site.

## Tests encore nécessaires

1. Deux appareils / navigateurs : création, invitation, comparaison d'empreinte, texte dans les deux sens.
2. Rechargement du navigateur et reprise du pair / clés depuis IndexedDB.
3. Messages envoyés pendant une coupure, redémarrage, ACK perdu puis resoumission sans duplication.
4. Séparation de plusieurs conversations et tentative de reprise d'un ancien lien.
5. Image, vidéo, vocal, taille maximale, interruption et confirmation de fin.
6. Expiration, effacement local et demande de suppression chiffrée.

Les tests CI actuels sont des contrôles statiques du code et de la syntaxe ; ils ne remplacent pas ces essais réels.
