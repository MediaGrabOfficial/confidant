# Confidant

Confidant est une messagerie privée en cours de développement, avec une interface web autonome et une coquille Android. L'objectif est de conserver messages et médias localement et de chiffrer les données avant leur envoi direct quand WebRTC établit une connexion.

## Projet indépendant

Ce dépôt est indépendant de CAM93. L'identité visuelle conserve l'ivoire, l'olive, le bronze et la typographie éditoriale de la base fournie.

## Éléments présents dans la base actuelle

- UI HTML/CSS/JavaScript en un seul fichier source : `confident.html`, copie de déploiement `site/index.html`.
- Primitives Web Crypto : ECDH P-256, HKDF-SHA-256 et AES-256-GCM.
- Échange texte, transferts de fichiers chiffrés en fragments, enregistrement vocal, QR d'invitation et stockage local IndexedDB.
- Coquille Android WebView ouvrant le site HTTPS et sollicitant les permissions du microphone si la page les demande.
- Workflows GitHub Actions pour publier le site et générer un APK de débogage.

**Important : la base reste un prototype non audité.** La liste multi-conversations, la reprise fiable après rechargement, la file d'attente hors ligne et les accusés de réception vérifiés restent à implémenter. Le statut actuel « Envoyé » ne prouve pas que le destinataire a reçu le message. Les commandes de suppression doivent être authentifiées avant d'être considérées fiables.

## Sans base de données distante — limites

Aucun contenu de message n'est stocké dans une base centrale par cette architecture. IndexedDB reste local à chaque navigateur/WebView. PeerJS fournit toutefois la signalisation nécessaire à l'établissement de WebRTC. Sans serveur TURN configuré, certains réseaux peuvent empêcher les connexions directes. Le site hébergé et les bibliothèques CDN sont aussi des dépendances externes.

L'historique du navigateur et celui de l'APK ne sont pas synchronisés. Un pair hors ligne ne peut pas recevoir immédiatement un message en l'absence d'un service intermédiaire de stockage.

## Publication web

Le workflow `Publier Confidant sur GitHub Pages` publie le dossier `site/` à chaque push sur `main`. Dans GitHub, ouvrir **Settings → Pages** et sélectionner **GitHub Actions** comme source si nécessaire.

URL prévue : https://mediagrabofficial.github.io/confidant/

## Compiler l'APK

Le workflow `Compiler l'APK Confidant` compile l'application et publie l'artefact `Confidant-debug-apk` pendant 14 jours. Il est téléchargeable depuis l'onglet **Actions**.

L'APK debug n'utilise pas encore de signature de publication persistante : des mises à jour peuvent demander une désinstallation. Une clé de signature stable sera nécessaire avant une distribution durable.

## Vérifications et priorités

Le CI vérifie la syntaxe JavaScript embarquée et quelques invariants, puis compile l'APK. Ce ne sont pas des tests WebRTC de bout en bout. Il reste à tester sur deux navigateurs/appareils : invitation, comparaison d'empreinte, échanges dans les deux sens, reconnexion, isolation des conversations, médias et suppression.

Priorités :
1. Modèle multi-conversations persistant avec historiques isolés.
2. Persistance sûre des informations de reprise et reconnexion progressive sans pairs concurrents.
3. File d'attente locale avec états honnêtes et expiration.
4. Accusés de réception protégés ; ne pas annoncer « reçu » avant une confirmation distante.
5. Protection des commandes de contrôle et tests réels entre deux appareils.
6. Réévaluation du stockage des clés et de l'expérience média.

Le protocole n'implémente pas de Double Ratchet et n'a pas été audité. Ne pas présenter ce prototype comme formellement vérifié.
