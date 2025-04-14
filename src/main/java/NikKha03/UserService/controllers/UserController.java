package NikKha03.UserService.controllers;

import NikKha03.UserService.DTO.UserKeycloakAdminDto;
import NikKha03.UserService.service.KeycloakUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/user_service/")
public class UserController {

    private final KeycloakUserService keycloakUserService;

    public UserController(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @GetMapping("/authorized")
    public Object getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("username: " + authentication.getName());
        return oidcUser;
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

    @GetMapping("/username/{username}")
    public ResponseEntity<UserKeycloakAdminDto> getUserByUsername(@PathVariable String username) {
        UserKeycloakAdminDto user = keycloakUserService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
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
