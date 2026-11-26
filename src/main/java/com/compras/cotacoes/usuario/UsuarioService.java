package com.compras.cotacoes.usuario;

import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.seguranca.PropriedadesSeguranca;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.usuario.dto.CriarUsuarioRequest;
import com.compras.cotacoes.usuario.dto.UsuarioResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropriedadesSeguranca propriedades;
    private final UsuarioAtual usuarioAtual;

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflitoException("usuario.username.duplicado", username);
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setNome(request.nome().trim());
        usuario.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim().toLowerCase(Locale.ROOT));
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil());
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário '{}' ({}) criado por '{}'", username, request.perfil(), usuarioAtual.username());
        return UsuarioResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByUsernameAsc().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse atual() {
        return usuarioRepository.findByUsernameIgnoreCase(usuarioAtual.username())
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("usuario.nao.encontrado", usuarioAtual.username()));
    }

    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("usuario.nao.encontrado", id));
        if (!ativo && usuario.getUsername().equalsIgnoreCase(usuarioAtual.username())) {
            throw new NegocioException("usuario.auto.inativacao");
        }
        usuario.setAtivo(ativo);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Primeira subida sem nenhum usuário: cria o ADMIN a partir de APP_ADMIN_SENHA, se informada. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        String senha = propriedades.adminSenha();
        if (senha == null || senha.isBlank()) {
            log.warn("Nenhum usuário cadastrado. Defina APP_ADMIN_SENHA para criar o administrador na próxima subida.");
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(propriedades.adminUsername().toLowerCase(Locale.ROOT));
        admin.setNome("Administrador");
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPerfil(Perfil.ADMIN);
        usuarioRepository.save(admin);
        log.info("Usuário administrador '{}' criado na primeira subida", admin.getUsername());
    }
}
