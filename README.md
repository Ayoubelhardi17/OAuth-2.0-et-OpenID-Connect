# Atelier OAuth 2.0 / OpenID Connect — React + Spring Boot + Keycloak

Structure :
```
atelier-oauth2/
├── docker-compose.yml     # Keycloak 26.8.1
├── demandes-api/          # Resource Server Spring Boot (port 8081)
└── demandes-front/        # Client React/Vite (port 5173)
```

## 1. Keycloak
```bash
docker compose up -d
```
Console : http://localhost:8080 (admin / admin), puis :

1. **Create realm** : `enset-realm`
2. **Clients > Create client** : OpenID Connect, Client ID `react-client`
   - Client authentication **Off**, Standard flow **On**, Implicit **Off**, Direct access grants **Off**
   - Root URL / Home URL : `http://localhost:5173/`
   - Valid redirect URIs : `http://localhost:5173/*`
   - Web origins : `http://localhost:5173`
   - Advanced > PKCE Code Challenge Method : `S256`
3. **Realm roles** : `USER`, `ADMIN`
4. **Users** (Credentials : Temporary = Off ; Role mapping = Realm roles) :
   | user  | mot de passe | rôles        |
   |-------|--------------|--------------|
   | alice | alice123     | USER         |
   | bob   | bob123       | USER, ADMIN  |

## 2. Backend
```bash
cd demandes-api
mvn spring-boot:run          # ou ./mvnw spring-boot:run si vous avez le wrapper
mvn test                     # tests de sécurité (n'exigent pas Keycloak)
```
Test rapide : http://localhost:8081/api/public → 200 ; /api/demandes sans token → 401.

## 3. Frontend
```bash
cd demandes-front
npm install
npm run dev
```
Ouvrir http://localhost:5173 → redirection vers Keycloak.

## 4. Récupérer un token pour Postman
Le client `react-client` n'autorise pas le Direct Access Grant (volontairement).
Dans l'app React connectée, ouvrir la console du navigateur et exécuter :
```js
// ne fonctionne que si keycloak est exposé temporairement, ex. window.kc = keycloak dans keycloak.js
copy(window.kc.token)
```
ou copier la valeur de l'en-tête `Authorization` dans l'onglet Network (requête vers :8081).
Ne jamais publier un token réel.

## 5. Tableau des tests
| # | Test | Attendu |
|---|------|---------|
| 1 | Alice : connexion, créer/voir ses demandes, pas de panneau Admin | OK |
| 2 | Bob : panneau Admin, voir toutes les demandes, Valider/Refuser | OK |
| 3 | Network : `Authorization: Bearer eyJ...` sur les appels :8081 | présent |
| 4 | GET /api/demandes sans token | 401 |
| 5 | GET /api/admin/demandes avec token Alice | 403 |
| 6 | GET /api/admin/demandes avec token Bob | 200 |
| 7 | Token modifié d'un caractère | 401 |

Les cas 4, 5, 6, 7 sont aussi automatisés dans `SecurityTests.java`.

## 6. Travail supplémentaire
- **Refuser** : bouton dans `App.jsx` → `PATCH /api/admin/demandes/{id}` avec `{"statut":"REFUSEE"}` (réservé ADMIN par `/api/admin/**`).
- **Profil** : `GET /api/profil` → `{ "id": <sub>, "username": <preferred_username> }`.
- **Tests de sécurité** : `demandes-api/src/test/.../SecurityTests.java`.

## 7. Analyse du JWT (https://jwt.io, token de test uniquement)
| Claim | Rôle |
|-------|------|
| `sub` | identifiant unique de l'utilisateur (propriétaire des demandes) |
| `iss` | émetteur : `http://localhost:8080/realms/enset-realm` |
| `exp` / `iat` | expiration / création |
| `realm_access.roles` | rôles applicatifs (USER, ADMIN) → `ROLE_*` via `jwtConverter()` |
| `scope` | scopes OAuth 2.0 (openid, profile, email) → `SCOPE_*` ; ≠ rôles |

## 8. Bilan
- Keycloak = Authorization Server + OpenID Provider
- React = client OAuth2/OIDC public (Authorization Code + PKCE S256)
- Spring Boot = Resource Server (validation signature/issuer/expiration des JWT)
- Spring Security = règles d'autorisation ; masquer le panneau Admin dans React n'est qu'un confort d'UI.
