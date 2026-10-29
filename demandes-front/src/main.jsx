import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import keycloak from './keycloak.js';

keycloak.init({
  onLoad: 'login-required',
  pkceMethod: 'S256',
  checkLoginIframe: false
}).then(() => {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
}).catch((error) => {
  console.error('Erreur de connexion Keycloak', error);
  document.getElementById('root').textContent =
    'Impossible de se connecter au serveur Keycloak.';
});
