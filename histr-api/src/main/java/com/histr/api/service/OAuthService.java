package com.histr.api.service;

import com.histr.api.enums.Provider;
import com.histr.api.enums.Role;
import com.histr.api.model.User;
import com.histr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(){
        return (request) -> {

            OidcUserService delegate = new OidcUserService();

            OidcUser oidcUser = delegate.loadUser(request);

            String provider = request
                    .getClientRegistration()
                    .getRegistrationId().toUpperCase();

            String email = oidcUser.getEmail();
            String name = oidcUser.getName();
            String picture = oidcUser.getPicture();

            if (email == null) {
                throw new OAuth2AuthenticationException("Email not found");
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setProvider(Provider.valueOf(provider));
                        newUser.setEmail(email);
                        newUser.setUsername(name);
                        newUser.setRole(Role.USER);
                        newUser.setPicture(picture);
                        return userRepository.save(newUser);
                    });

            if(!user.getProvider().equals(Provider.valueOf(provider))){
                throw new OAuth2AuthenticationException("User account created with another provider");
            }

            return new DefaultOidcUser(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")), oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub"
            );
        };
    }

}
