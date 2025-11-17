package NikKha03.UserService.controllers;

import NikKha03.UserService.DTO.UserKeycloakAdminDto;
import NikKha03.UserService.service.KeycloakUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "user_controller")
@RequestMapping("/user_service/")
public class UserController {

    // Spring автоматически сохраняет: access token и refresh token
    // в OAuth2AuthorizedClientService после успешной OAuth2/OIDC авторизации (то есть когда сработал oauth2Login)
    @Autowired
    private OAuth2AuthorizedClientService clientService;

    private final KeycloakUserService keycloakUserService;

    public UserController(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @GetMapping("/signin")
    public ResponseEntity<Object> signin(@RequestParam String username, @RequestParam String password) {
        return ResponseEntity.ok(keycloakUserService.signIn(username, password));
    }

    @GetMapping("/authorized-jwt")
    public Object getUser(Authentication authentication) {
        return authentication.getPrincipal();
    }

    @GetMapping("/authorized")
    public Object getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        // @TODO Это access токен, его надо сохранить в память frontend-a и с ним делать запросы на api
        // если токен недействителен, то делаем запрос на refresh
        System.out.println(oidcUser.getIdToken().getTokenValue());
        return oidcUser;
    }

    @GetMapping("/refresh")
    public Object getRefreshToken(Authentication auth) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("keycloak", auth.getName());

        if (client != null && client.getRefreshToken() != null) {
            // @TODO Надо не refresh возвращать, а делать запрос на обновление access и его возвращать
            return client.getRefreshToken().getTokenValue();
        }

        return "no refresh token";
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<?> logout(@PathVariable String userId) {
        // userId это sub
        return ResponseEntity.ok(keycloakUserService.logout(userId));
    }

    @GetMapping
    public ResponseEntity<List<UserKeycloakAdminDto>> getAllUsers() {
        return ResponseEntity.ok(keycloakUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserKeycloakAdminDto> getUserById(@PathVariable String id) {
        UserKeycloakAdminDto user = keycloakUserService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<UserKeycloakAdminDto> getUserByUsername(@PathVariable String username) {
        UserKeycloakAdminDto user = keycloakUserService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<Object> getUsersByUsername(@RequestBody List<String> usernames) {
        if (usernames == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> result = new HashMap<>();
        for (String username : usernames) {
            UserKeycloakAdminDto user = keycloakUserService.getUserByUsername(username);
            result.put(username, user.getFirstName() + " " + user.getLastName());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody UserKeycloakAdminDto userDto) {
        String userId = keycloakUserService.createUser(userDto);
        return ResponseEntity.created(URI.create("/user_service/" + userId)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable String id, @RequestBody UserKeycloakAdminDto userDto) {
        try {
            keycloakUserService.updateUser(id, userDto);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            keycloakUserService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
