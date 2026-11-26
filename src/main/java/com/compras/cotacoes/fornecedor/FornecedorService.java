package com.compras.cotacoes.fornecedor;

import com.compras.cotacoes.fornecedor.dto.CriarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorMapper fornecedorMapper;

    @Transactional
    public FornecedorResponse criar(CriarFornecedorRequest request) {
        String documento = normalizarDocumento(request.documento());
        if (fornecedorRepository.existsByDocumento(documento)) {
            throw new ConflitoException("fornecedor.documento.duplicado", documento);
        }
        Fornecedor fornecedor = fornecedorMapper.paraEntidade(request);
        fornecedor.setRazaoSocial(request.razaoSocial().trim());
        fornecedor.setDocumento(documento);
        fornecedor.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        return fornecedorMapper.paraResposta(fornecedorRepository.save(fornecedor));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listar(Boolean ativo) {
        List<Fornecedor> fornecedores = ativo == null
                ? fornecedorRepository.findAllByOrderByRazaoSocialAsc()
                : fornecedorRepository.findByAtivoOrderByRazaoSocialAsc(ativo);
        return fornecedorMapper.paraRespostas(fornecedores);
    }

    @Transactional
    public FornecedorResponse atualizarStatus(Long id, boolean ativo) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("fornecedor.nao.encontrado", id));
        fornecedor.setAtivo(ativo);
        return fornecedorMapper.paraResposta(fornecedorRepository.save(fornecedor));
    }

    /** Aceita "11.222.333/0001-81" ou "11222333000181" e guarda só os dígitos. */
    static String normalizarDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }
}
