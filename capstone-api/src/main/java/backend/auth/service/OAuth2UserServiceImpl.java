package backend.auth.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.dto.CustomOAuth2User;
import backend.user.entity.SocialProviderType;
import backend.user.entity.UserEntity;
import backend.user.entity.UserRoleType;
import backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public OAuth2UserServiceImpl(
		UserRepository userRepository,
		@Lazy BCryptPasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		Map<String, Object> attributes = oAuth2User.getAttributes();

		String email;
		String nickname;
		SocialProviderType providerType;

		if ("naver".equals(registrationId)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
			email = (String) naverResponse.get("email");
			nickname = (String) naverResponse.get("nickname");
			providerType = SocialProviderType.NAVER;
		} else {
			email = (String) attributes.get("email");
			nickname = (String) attributes.get("name");
			providerType = SocialProviderType.GOOGLE;
		}

		SocialProviderType finalProviderType = providerType;
		UserEntity user = userRepository.findByEmail(email)
			.orElseGet(() -> {
				UserEntity newUser = UserEntity.builder()
					.email(email)
					.password(passwordEncoder.encode("SOCIAL_" + email))
					.nickname(nickname != null ? nickname : email.split("@")[0])
					.isSocial(true)
					.isVerified(true)
					.socialProviderType(finalProviderType)
					.roleType(UserRoleType.USER)
					.build();
				return userRepository.save(newUser);
			});

		return new CustomOAuth2User(
			user.getEmail(),
			user.getNickname(),
			user.getRoleType().name(),
			registrationId,
			attributes,
			Collections.singletonList(
				new SimpleGrantedAuthority("ROLE_" + user.getRoleType().name())
			)
		);
	}
}
