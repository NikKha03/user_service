package NikKha03.UserService.service;

import NikKha03.UserService.DTO.UserKeycloakAdminDto;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KeycloakUserService {

    private final Keycloak keycloak;

    public KeycloakUserService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Value("${keycloak.admin.target-realm}")
    private String realm;

    @Value("#{'${spring.security.oauth2.client.registration.keycloak.client-id}'}")
    private String clientId;

    @Value("#{'${spring.security.oauth2.client.registration.keycloak.client-secret}'}")
    private String clientSecret;

    private RestTemplate restTemplate = new RestTemplate();

    public Object signIn(String username, String password) {
        // Создаем headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Создаем form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);

        // Создаем HttpEntity
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        try {
            return restTemplate.exchange(
                    "https://keycloak.sharpbubbles.online/realms/NikKha03/protocol/openid-connect/token",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            ).getBody();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String logout(String userId) {
        try {
            keycloak.realm(realm).users().get(userId).logout();
            return "Сеансы пользователя завершены";
        } catch (Exception e) {
            return "Что-то пошло не так :(";
        }
    }

    public List<UserKeycloakAdminDto> getAllUsers() {
        return keycloak.realm(realm).users().list().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    public UserKeycloakAdminDto getUserById(String id) {
        UserRepresentation user = keycloak.realm(realm).users().get(id).toRepresentation();
        return mapToUserDto(user);
    }

    public UserKeycloakAdminDto getUserByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users().search(username, true);
        if (users.isEmpty()) {
            return null;
        }
        return mapToUserDto(users.get(0));
    }

    public String createUser(UserKeycloakAdminDto userDto) {
        UserRepresentation user = mapToUserRepresentation(userDto);

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() == 201) {
                String userId = extractCreatedId(response);

                if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                    resetPassword(userId, userDto.getPassword());
                }

                if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
                    assignRoles(userId, userDto.getRoles());
                }

                return userId;
            } else {
                throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
            }
        }
    }

    public void updateUser(String userId, UserKeycloakAdminDto userDto) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = mapToUserRepresentation(userDto);
        user.setId(userId);

        userResource.update(user);

        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            resetPassword(userId, userDto.getPassword());
        }

        if (userDto.getRoles() != null) {
            updateUserRoles(userId, userDto.getRoles());
        }
    }

    public void deleteUser(String userId) {
        keycloak.realm(realm).users().delete(userId);
    }

    private String extractCreatedId(Response response) {
        String locationHeader = response.getHeaderString("Location");
        if (locationHeader != null) {
            return locationHeader.replaceAll(".*/([^/]+)$", "$1");
        }
        return null;
    }

    private void resetPassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        keycloak.realm(realm).users().get(userId).resetPassword(credential);
    }

    private void assignRoles(String userId, List<String> roleNames) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        List<RoleRepresentation> rolesToAdd = roleNames.stream()
                .map(roleName -> realmResource.roles().get(roleName).toRepresentation())
                .collect(Collectors.toList());

        userResource.roles().realmLevel().add(rolesToAdd);
    }

    private void updateUserRoles(String userId, List<String> newRoleNames) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        // Get existing roles
        List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();

        // Remove all existing roles
        if (!existingRoles.isEmpty()) {
            userResource.roles().realmLevel().remove(existingRoles);
        }

        // Add new roles
        if (newRoleNames != null && !newRoleNames.isEmpty()) {
            List<RoleRepresentation> rolesToAdd = newRoleNames.stream()
                    .map(roleName -> realmResource.roles().get(roleName).toRepresentation())
                    .collect(Collectors.toList());

            userResource.roles().realmLevel().add(rolesToAdd);
        }
    }

    private UserKeycloakAdminDto mapToUserDto(UserRepresentation user) {
        if (user == null) {
            return null;
        }

        UserKeycloakAdminDto userDto = new UserKeycloakAdminDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEnabled(user.isEnabled());
        userDto.setEmailVerified(user.isEmailVerified());
        userDto.setAttributes(user.getAttributes());

        // Get user roles
        if (user.getId() != null) {
            List<String> realmRoles = keycloak.realm(realm).users().get(user.getId())
                    .roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
            userDto.setRoles(realmRoles);
        }

        return userDto;
    }

    private UserRepresentation mapToUserRepresentation(UserKeycloakAdminDto userDto) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEnabled(userDto.isEnabled());
        user.setEmailVerified(userDto.isEmailVerified());
        user.setAttributes(userDto.getAttributes());

        return user;
    }
}