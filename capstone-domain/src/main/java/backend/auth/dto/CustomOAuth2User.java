package backend.auth.dto;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

	private final String email;
	private final String nickname;
	private final String role;
	private final String provider;
	private final Map<String, Object> attributes;
	private final Collection<? extends GrantedAuthority> authorities;
	private final boolean isNewUser;

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getName() {
		return email;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public String getRole() {
		return role;
	}

	public String getProvider() {
		return provider;
	}

	public boolean isNewUser() {
		return isNewUser;
	}
}
