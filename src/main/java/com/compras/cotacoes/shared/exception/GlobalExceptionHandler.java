package com.compras.cotacoes.shared.exception;

import com.compras.cotacoes.shared.i18n.MensagemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MensagemService mensagens;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroRespostaDTO> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroRespostaDTO.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroRespostaDTO.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroRespostaDTO.CampoInvalido::campo))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, mensagens.obter("erro.validacao"), request, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroRespostaDTO> validacaoParametros(HandlerMethodValidationException ex,
                                                               HttpServletRequest request) {
        List<ErroRespostaDTO.CampoInvalido> campos = ex.getAllErrors().stream()
                .map(erro -> new ErroRespostaDTO.CampoInvalido(
                        erro.getCodes() != null && erro.getCodes().length > 0 ? erro.getCodes()[0] : "parametro",
                        erro.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, mensagens.obter("erro.validacao"), request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroRespostaDTO> violacaoRestricao(ConstraintViolationException ex,
                                                             HttpServletRequest request) {
        List<ErroRespostaDTO.CampoInvalido> campos = ex.getConstraintViolations().stream()
                .map(v -> new ErroRespostaDTO.CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, mensagens.obter("erro.validacao"), request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroRespostaDTO> corpoInvalido(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, mensagens.obter("erro.requisicao.invalida"), request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroRespostaDTO> tipoInvalido(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        String mensagem = mensagens.obter("erro.parametro.invalido", ex.getName(), String.valueOf(ex.getValue()));
        return responder(HttpStatus.BAD_REQUEST, mensagem, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroRespostaDTO> parametroAusente(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        List<ErroRespostaDTO.CampoInvalido> campos = List.of(
                new ErroRespostaDTO.CampoInvalido(ex.getParameterName(), mensagens.obter("validacao.obrigatorio")));
        return responder(HttpStatus.BAD_REQUEST, mensagens.obter("erro.validacao"), request, campos);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroRespostaDTO> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest request) {
        ResponseEntity<ErroRespostaDTO> resposta = responder(HttpStatus.METHOD_NOT_ALLOWED,
                mensagens.obter("erro.metodo.nao.suportado", ex.getMethod()), request, List.of());
        if (ex.getSupportedHttpMethods() == null) {
            return resposta;
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(ex.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new))
                .body(resposta.getBody());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroRespostaDTO> midiaNaoSuportada(HttpMediaTypeNotSupportedException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                mensagens.obter("erro.midia.nao.suportada", String.valueOf(ex.getContentType())), request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErroRespostaDTO> midiaNaoAceitavel(HttpMediaTypeNotAcceptableException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.NOT_ACCEPTABLE, mensagens.obter("erro.midia.nao.aceitavel"), request, List.of());
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErroRespostaDTO> naoAutenticado(NaoAutenticadoException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, traduzir(ex), request, List.of());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroRespostaDTO> muitasTentativas(MuitasTentativasException ex, HttpServletRequest request) {
        ResponseEntity<ErroRespostaDTO> resposta = responder(HttpStatus.TOO_MANY_REQUESTS, traduzir(ex), request, List.of());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(Math.max(1, ex.getAguardar().toSeconds())))
                .body(resposta.getBody());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroRespostaDTO> naoEncontrado(RecursoNaoEncontradoException ex,
                                                         HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, traduzir(ex), request, List.of());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroRespostaDTO> negocio(NegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, traduzir(ex), request, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroRespostaDTO> conflito(ConflitoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, traduzir(ex), request, List.of());
    }

    /** Rede de segurança para corridas que passam pela checagem prévia (dois cadastros simultâneos do mesmo SKU). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroRespostaDTO> integridade(DataIntegrityViolationException ex,
                                                       HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, mensagens.obter("erro.conflito.integridade"), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroRespostaDTO> rotaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroRespostaDTO> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, mensagens.obter("erro.interno"), request, List.of());
    }

    private String traduzir(ExcecaoLocalizada ex) {
        return mensagens.obter(ex.getChave(), ex.getArgumentos());
    }

    private ResponseEntity<ErroRespostaDTO> responder(HttpStatus status, String mensagem, HttpServletRequest request,
                                                      List<ErroRespostaDTO.CampoInvalido> campos) {
        ErroRespostaDTO corpo = new ErroRespostaDTO(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, request.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
