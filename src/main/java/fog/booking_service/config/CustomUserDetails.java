package fog.booking_service.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private final String sub;

    public CustomUserDetails(String username, String sub, Collection<? extends GrantedAuthority> authorities) {
        super(username, "", authorities);
        this.sub = sub;
    }

    public String getSub() {
        return sub;
    }
}
