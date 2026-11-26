package com.compras.cotacoes.fornecedor;

import com.compras.cotacoes.fornecedor.dto.AtualizarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.CriarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.web.PaginaResponse;
import com.compras.cotacoes.shared.web.Paginacao;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorMapper fornecedorMapper;
    private final HistoricoService historico;

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
        Fornecedor salvo = fornecedorRepository.save(fornecedor);
        historico.registrar(EntidadeHistorico.FORNECEDOR, salvo.getId(), AcaoHistorico.FORNECEDOR_CRIADO, documento);
        return fornecedorMapper.paraResposta(salvo);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscar(Long id) {
        return fornecedorMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<FornecedorResponse> listar(Boolean ativo, String busca, Integer pagina, Integer tamanho) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        return PaginaResponse.de(fornecedorRepository.buscar(ativo, termo,
                Paginacao.de(pagina, tamanho, Sort.by("razaoSocial", "id"))), fornecedorMapper::paraResposta);
    }

    /** O documento é a identidade fiscal e não muda; razão social, e-mail e prazo podem mudar. */
    @Transactional
    public FornecedorResponse atualizar(Long id, AtualizarFornecedorRequest request) {
        Fornecedor fornecedor = buscarEntidade(id);
        fornecedor.setRazaoSocial(request.razaoSocial().trim());
        fornecedor.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        fornecedor.setPrazoMedioDias(request.prazoMedioDias());
        historico.registrar(EntidadeHistorico.FORNECEDOR, id, AcaoHistorico.FORNECEDOR_ATUALIZADO, fornecedor.getRazaoSocial());
        return fornecedorMapper.paraResposta(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public FornecedorResponse atualizarStatus(Long id, boolean ativo) {
        Fornecedor fornecedor = buscarEntidade(id);
        if (fornecedor.isAtivo() != ativo) {
            fornecedor.setAtivo(ativo);
            historico.registrar(EntidadeHistorico.FORNECEDOR, id,
                    ativo ? AcaoHistorico.FORNECEDOR_ATIVADO : AcaoHistorico.FORNECEDOR_INATIVADO, fornecedor.getRazaoSocial());
        }
        return fornecedorMapper.paraResposta(fornecedorRepository.save(fornecedor));
    }

    /** Aceita "11.222.333/0001-81" ou "11222333000181" e guarda só os dígitos. */
    static String normalizarDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }

    private Fornecedor buscarEntidade(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("fornecedor.nao.encontrado", id));
    }
}
