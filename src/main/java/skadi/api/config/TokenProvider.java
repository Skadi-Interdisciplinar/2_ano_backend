package skadi.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.Authenticator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class TokenProvider {

    @Value("${jwt.expiration}")
    private Long expirationTime;

    @Value("${jwt.key}")
    private String key;


    public String generateToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return buildToken(userDetails.getUsername());
        }
        return buildToken(authentication.getName());
    }

    public String buildToken(String username){
        Instant now = Instant.now();

        Instant expiration = now.plusSeconds(expirationTime);

        return Jwts
                .builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSecretKey())
                .compact();

        
    }

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isTokenValid(String token) throws ExpiredJwtException {
        try{
            getClaims(token);
            return true;
        }
        catch(ExpiredJwtException e){
            throw new ExpiredJwtException(null,null,"Token inválido");
        }

        catch(Exception e){
            return false;

        }
    }

    private Claims getClaims(String token){
        //assinatura do token
        return Jwts
                .parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();



    }

    public String getUsername(String token){
        return getClaims(token).getSubject();
    }

}
