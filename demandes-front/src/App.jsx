import { useEffect, useState } from 'react';
import keycloak from './keycloak.js';
import { appelerApi } from './api.js';
import './App.css';

export default function App() {
  const [objet, setObjet] = useState('');
  const [mesDemandes, setMesDemandes] = useState([]);
  const [toutes, setToutes] = useState([]);
  const [message, setMessage] = useState('');

  const roles = keycloak.tokenParsed?.realm_access?.roles || [];
  const isAdmin = roles.includes('ADMIN');
  const username = keycloak.tokenParsed?.preferred_username || 'Utilisateur';

  async function charger() {
    try {
      setMesDemandes(await appelerApi('/api/demandes'));
      if (isAdmin) {
        setToutes(await appelerApi('/api/admin/demandes'));
      }
    } catch (e) {
      setMessage('Erreur : ' + e.message);
    }
  }

  useEffect(() => {
    charger();
  }, []);

  async function creer(e) {
    e.preventDefault();
    try {
      await appelerApi('/api/demandes', {
        method: 'POST',
        body: JSON.stringify({ objet })
      });
      setObjet('');
      setMessage('Demande créée.');
      await charger();
    } catch (e) {
      setMessage('Erreur : ' + e.message);
    }
  }

  // Valider (VALIDEE) ou Refuser (REFUSEE)
  async function changerStatut(id, statut) {
    try {
      await appelerApi(`/api/admin/demandes/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ statut })
      });
      setMessage(statut === 'VALIDEE' ? 'Demande validée.' : 'Demande refusée.');
      await charger();
    } catch (e) {
      setMessage('Erreur : ' + e.message);
    }
  }

  return (
    <main>
      <header>
        <h1>Gestion des demandes</h1>
        <p>Connecté : <strong>{username}</strong></p>
        <p>Rôles : {roles.join(', ')}</p>
        <button onClick={() => keycloak.logout({ redirectUri: 'http://localhost:5173/' })}>
          Se déconnecter
        </button>
      </header>

      <section>
        <h2>Créer une demande</h2>
        <form onSubmit={creer}>
          <input
            value={objet}
            onChange={e => setObjet(e.target.value)}
            placeholder="Objet de la demande"
            required
          />
          <button type="submit">Envoyer</button>
        </form>
      </section>

      <section>
        <h2>Mes demandes</h2>
        <ul>
          {mesDemandes.map(d => (
            <li key={d.id}>#{d.id} - {d.objet} ({d.statut})</li>
          ))}
        </ul>
      </section>

      {isAdmin && (
        <section>
          <h2>Administration : toutes les demandes</h2>
          <ul>
            {toutes.map(d => (
              <li key={d.id}>
                #{d.id} - {d.objet} ({d.statut})
                {d.statut !== 'VALIDEE' && (
                  <button onClick={() => changerStatut(d.id, 'VALIDEE')}>Valider</button>
                )}
                {d.statut !== 'REFUSEE' && (
                  <button className="danger" onClick={() => changerStatut(d.id, 'REFUSEE')}>
                    Refuser
                  </button>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      <p role="status">{message}</p>
    </main>
  );
}
