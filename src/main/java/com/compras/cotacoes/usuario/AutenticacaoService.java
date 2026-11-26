package com.compras.cotacoes.usuario;

import com.compras.cotacoes.shared.exception.MuitasTentativasException;
import com.compras.cotacoes.shared.exception.NaoAutenticadoException;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import com.compras.cotacoes.shared.seguranca.PropriedadesSeguranca;
import com.compras.cotacoes.shared.seguranca.ProtecaoForcaBruta;
import com.compras.cotacoes.shared.seguranca.SegurancaConfig;
import com.compras.cotacoes.usuario.dto.LoginRequest;
import com.compras.cotacoes.usuario.dto.TokenResponse;
import com.compras.cotacoes.usuario.dto.UsuarioResponse;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final ProtecaoForcaBruta protecao;
    private final PropriedadesSeguranca propriedades;
    private final MetricasCompras metricas;
    private final Clock clock;

    /**
     * Hash BCrypt de uma senha aleatória: quando o usuário não existe, a senha é comparada com ele mesmo assim,
     * para o tempo de resposta não revelar quais usernames existem.
     */
    private volatile String hashFicticio;

    @PostConstruct
    void prepararHashFicticio() {
        hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String username = request.username().trim();
        protecao.bloqueio(username).ifPresent(restante -> {
            throw new MuitasTentativasException(restante);
        });

        Optional<Usuario> encontrado = usuarioRepository.findByUsernameIgnoreCase(username);
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash);
        if (encontrado.isEmpty() || !senhaConfere || !encontrado.get().isAtivo()) {
            protecao.registrarFalha(username);
            metricas.loginFalho();
            log.warn("Login recusado para '{}'", username.replaceAll("[^A-Za-z0-9._-]", "?"));
            throw new NaoAutenticadoException("auth.credenciais.invalidas");
        }
        protecao.registrarSucesso(username);
        Usuario usuario = encontrado.get();

        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(propriedades.jwtExpiracao());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("compras-cotacoes")
                .subject(usuario.getUsername())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(SegurancaConfig.CLAIM_PERFIL, usuario.getPerfil().name())
                .claim(SegurancaConfig.CLAIM_NOME, usuario.getNome())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        log.info("Login de '{}' ({})", usuario.getUsername(), usuario.getPerfil());
        return new TokenResponse(token, "Bearer", expiraEm, UsuarioResponse.de(usuario));
    }
}
