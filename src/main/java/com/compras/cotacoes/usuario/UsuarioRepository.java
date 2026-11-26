package com.compras.cotacoes.usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<Usuario> findAllByOrderByUsernameAsc();

    List<Usuario> findByPerfilInAndAtivoTrueAndEmailIsNotNull(Collection<Perfil> perfis);
}
