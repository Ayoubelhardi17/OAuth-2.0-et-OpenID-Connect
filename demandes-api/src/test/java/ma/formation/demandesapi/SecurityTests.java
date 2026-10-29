package ma.formation.demandesapi;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired MockMvc mvc;

    // Évite tout appel réel à Keycloak : le décodeur rejette tout jeton "réel"
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Jeton invalide"));
    }

    @Test
    void apiPubliqueAccessibleSansToken() throws Exception {
        mvc.perform(get("/api/public")).andExpect(status().isOk());
    }

    // 1. Requête sans token refusée
    @Test
    void sansTokenRefuse() throws Exception {
        mvc.perform(get("/api/demandes")).andExpect(status().isUnauthorized());
    }

    // 2. Requête avec token invalide refusée
    @Test
    void tokenInvalideRefuse() throws Exception {
        mvc.perform(get("/api/demandes").header("Authorization", "Bearer eyJ.token.invalide"))
           .andExpect(status().isUnauthorized());
    }

    // 3. USER ne peut pas utiliser l'API d'administration
    @Test
    void userNePeutPasModifierViaApiAdmin() throws Exception {
        mvc.perform(patch("/api/admin/demandes/1")
                .with(jwt().authorities(() -> "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"VALIDEE\"}"))
           .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/demandes").with(jwt().authorities(() -> "ROLE_USER")))
           .andExpect(status().isForbidden());
    }

    // 4. ADMIN peut modifier le statut (valider puis refuser)
    @Test
    void adminPeutModifierLeStatut() throws Exception {
        mvc.perform(post("/api/demandes")
                .with(jwt().jwt(j -> j.subject("alice-id")).authorities(() -> "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objet\":\"Test\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        mvc.perform(patch("/api/admin/demandes/1")
                .with(jwt().authorities(() -> "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"REFUSEE\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.statut").value("REFUSEE"));
    }

    @Test
    void profilExtraitDuJwt() throws Exception {
        mvc.perform(get("/api/profil")
                .with(jwt().jwt(j -> j.subject("abc-123").claim("preferred_username", "alice"))
                           .authorities(() -> "ROLE_USER")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value("abc-123"))
           .andExpect(jsonPath("$.username").value("alice"));
    }
}
