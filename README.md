# Confidant

Messagerie privée peer-to-peer, sans compte obligatoire ni base de données distante de messages.

## État actuel

Ce dépôt démarre à partir de la version HTML fournie. Il conserve l'identité visuelle ivoire / olive / bronze et les primitives cryptographiques présentes (ECDH P-256, HKDF-SHA-256, AES-256-GCM).

**Cette base est un prototype, pas encore une messagerie stabilisée ou auditée.** Le travail prioritaire est la reprise des conversations, la séparation des historiques, les accusés de réception vérifiables, la file d'attente locale et les tests entre deux appareils.

## Structure

- `site/index.html` : version déployée du client web.
- `confident.html` : copie de travail autonome de la base.
- `app/` : application Android WebView ouvrant le client en HTTPS.
- `.github/workflows/pages.yml` : publication GitHub Pages.
- `.github/workflows/build-apk.yml` : compilation automatique d'un APK debug.

## Dépendances et limites

- PeerJS / WebRTC assurent la signalisation et la connexion directe. Il n'y a pas de serveur de stockage des messages.
- Les appareils doivent tout de même joindre un service de signalisation. Sans TURN, certaines connexions réseau peuvent échouer.
- Les historiques IndexedDB restent séparés entre le navigateur et le WebView Android.
- L'APK debug n'a pas une signature de publication persistante : les mises à jour peuvent nécessiter une désinstallation.
- Aucun audit de sécurité n'a été réalisé ; ne pas utiliser ce prototype pour des communications à très haut risque.

## Publication web

GitHub Pages doit être configuré pour utiliser **GitHub Actions** comme source dans les paramètres Pages du dépôt. Le workflow publie le dossier `site/` à chaque push sur `main`.

## Construire l'APK

Le workflow **Compiler l'APK Confidant** s'exécute à chaque push sur `main`. À la fin, téléchargez l'artefact `Confidant-debug-apk` depuis l'onglet Actions.

L'APK ouvre `https://mediagrabofficial.github.io/confidant/`. La première compilation peut réussir avant que GitHub Pages ait terminé sa première publication ; l'application ne pourra alors charger le site qu'une fois celui-ci publié.

## Plan de stabilisation

1. Tests cryptographiques et vérifications de protocole.
2. Modèle multi-conversations persistant et navigation.
3. File d'attente locale, reconnexion progressive et états honnêtes des messages.
4. Accusés de réception chiffrés/authentifiés et contrôles de conversation protégés.
5. Tests de bout en bout sur deux navigateurs/appareils, puis amélioration du stockage et de l'expérience Android.
