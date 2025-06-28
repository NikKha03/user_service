package NikKha03.UserService.config;

import NikKha03.UserService.clients.TaskServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final TaskServiceClient taskServiceClient;

    public SecurityConfig(TaskServiceClient taskServiceClient) {
        this.taskServiceClient = taskServiceClient;
    }

    /*
    @Value("#{'${dop-urls.allowed-origins}'.split(', ')}")
    private List<String> allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5175", "https://tracker.sharpbubbles.ru", "https://api.sharpbubbles.ru"));
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);

        return urlBasedCorsConfigurationSource;
    }
    */

    @Value("#{'${spring.security.oauth2.client.provider.keycloak.user-name-attribute}'}")
    private String attribute;

    @Bean()
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resource -> resource
                        .jwt(Customizer.withDefaults())
                )
                .oauth2Login(Customizer.withDefaults())
        ;

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtAuthenticationConverter.setPrincipalClaimName(attribute);
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            var roles = jwt.getClaimAsStringList("groups");

            return Stream.concat(authorities.stream(),
                            Optional.ofNullable(roles)
                                    .orElseGet(List::of)
                                    .stream()
                                    .filter(role -> role.startsWith("ROLE_"))
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();
        });
        return jwtAuthenticationConverter;
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        OidcUserService oidcUserService = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = oidcUserService.loadUser(userRequest);
            List<GrantedAuthority> authorities =
                    Stream.concat(oidcUser.getAuthorities().stream(),
                                    Optional.ofNullable(oidcUser.getClaimAsStringList("groups"))
                                            .orElseGet(List::of)
                                            .stream()
                                            .filter(role -> role.startsWith("ROLE_"))
                                            .map(SimpleGrantedAuthority::new)
                                            .map(GrantedAuthority.class::cast))
                            .toList();

            //TODO: Убрать эту логику, так как сервис может использоваться различными приложениями/системами
            try {
                if (!taskServiceClient.isDataHave(oidcUser.getPreferredUsername())) {
                    Map<String, String> result = new HashMap<>();
                    result.put("username", oidcUser.getPreferredUsername());
                    result.put("userId", oidcUser.getUserInfo().getSubject());
                    taskServiceClient.pushUserInTaskService(result);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), "upn");
        };
    }
}
