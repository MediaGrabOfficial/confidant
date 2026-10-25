# Analyse de Confidant — état au 25 octobre 2026

## Fondations reprises et ajoutées dans le dépôt

La base fournie comprend la UI HTML/CSS/JS autonome, les fonctions de cryptographie Web Crypto, l'invitation PeerJS, le texte, les médias en fragments, le vocal, QRCode.js et le stockage IndexedDB. Le projet contient désormais la structure Gradle, la coquille Android WebView, les workflows Pages/APK et des contrôles statiques.

Les ajouts de code dans la branche principale comprennent la liste de plusieurs conversations locales, les noms personnalisés, la persistance des identifiants PeerJS et des objets CryptoKey dans IndexedDB, la reprise d'une conversation, une file locale de texte, des tentatives de reconnexion bornées, des ACK texte chiffrés et des commandes de contrôle chiffrées.

## Ce que les vérifications démontrent

- Le JavaScript intégré au HTML est analysé par `vm.Script` sans erreur de syntaxe.
- Les vérifications statiques trouvent les URL CDN corrigées, les primitives cryptographiques attendues, IndexedDB, le système de conversations, la file d'attente et les contrôleurs d'ACK.
- Gradle compile l'APK debug et publie un artefact GitHub Actions.

## Ce que les vérifications ne démontrent pas

Aucun essai réel n'a encore confirmé l'échange entre deux appareils. Il reste donc à vérifier : négociation WebRTC, reprise des clés CryptoKey depuis IndexedDB sur les appareils ciblés, ré-authentification/reconnexion, file texte et ACK perdus, plusieurs conversations, médias complets et suppression.

## Risques et limites restantes

1. Les conversations supplémentaires sont conservées, mais une seule connexion est active à la fois. L'appareil doit ouvrir une conversation pour la reprendre ; aucune réception en arrière-plan n'est implémentée.
2. Seuls les messages texte sont mis en attente hors connexion. Les médias ne sont pas conservés dans une file d'envoi automatique.
3. Les clés ECDH restent stockées localement en IndexedDB sans chiffrement par PIN. Les médias enregistrés sont des Blob locaux en clair au repos.
4. Il n'y a pas de Double Ratchet ; les clés ECDH peuvent être réutilisées lors des reprises. Le protocole n'a pas fait l'objet d'un audit indépendant.
5. PeerJS reste un service de signalisation tiers et aucun TURN n'est configuré.
6. L'APK est un conteneur WebView qui dépend du site en HTTPS. GitHub Pages doit être activé manuellement : Settings → Pages → Build and deployment → GitHub Actions. La connexion GitHub utilisée par l'automatisation ne peut pas activer Pages elle-même.
7. L'APK debug n'a pas de signature permanente de publication.

## Plan de test suivant

- Tester en deux navigateurs avec création, invitation, échanges dans les deux sens et comparaison d'empreinte indépendante.
- Recharger chaque côté puis vérifier que peer IDs et clés sont réutilisés sans créer de faux état « connecté ».
- Déconnecter le canal après send et avant ACK, puis vérifier que la mise en attente/reprise ne duplique pas l'historique.
- Ouvrir/sélectionner plusieurs conversations et vérifier l'isolation des messages et clés.
- Tester image, vidéo, vocal, taille maximale, transfert interrompu et ack média.
- Tester la suppression/expiration localement et le contrôle de suppression chiffré.

Ne pas présenter l'application comme une messagerie auditée ni comme une garantie de livraison hors ligne. Une file locale n'est pas une boîte aux lettres distante.
