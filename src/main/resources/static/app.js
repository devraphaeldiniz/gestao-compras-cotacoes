'use strict';

/* =====================================================================
   Compras & Cotações: interface web (PT/EN) sobre a API REST.
   Sem framework e sem script inline (CSP 'self'); token JWT em sessionStorage.
   ===================================================================== */

const I18N = {
  pt: {
    'login.sub': 'Entre com seu usuário para acessar o sistema.', 'login.usuario': 'Usuário', 'login.senha': 'Senha', 'login.entrar': 'Entrar',
    'login.demo': 'Demonstração: comprador / comprador123 · aprovador / aprovador123 · admin / admin123456',
    'aba.produtos': 'Produtos', 'aba.fornecedores': 'Fornecedores', 'aba.cotacoes': 'Cotações', 'aba.pedidos': 'Pedidos de compra', 'aba.usuarios': 'Usuários',
    'kpi.necessidade': 'Produtos no estoque mínimo', 'kpi.rascunhos': 'Cotações em rascunho', 'kpi.abertas': 'Cotações abertas', 'kpi.emitidos': 'Pedidos a receber',
    'produtos.titulo': 'Produtos e estoque', 'produtos.sub': 'Quando o saldo chega ao estoque mínimo, o produto entra automaticamente no rascunho de reposição.',
    'produtos.novo': 'Novo produto', 'produtos.movimentar': 'Movimentar estoque', 'produtos.filtro': 'Só com necessidade de compra',
    'fornecedores.titulo': 'Fornecedores', 'fornecedores.sub': 'Propostas de fornecedores inativos deixam de valer na decisão.', 'fornecedores.novo': 'Novo fornecedor',
    'cotacoes.titulo': 'Cotações', 'cotacoes.sub': 'Clique numa cotação para abri-la, registrar propostas e decidir a vencedora.', 'cotacoes.nova': '+ Nova cotação',
    'pedidos.titulo': 'Pedidos de compra', 'pedidos.sub': 'Gerados na decisão. O recebimento (total ou parcial) dá entrada no estoque e recalcula o custo médio.',
    'usuarios.titulo': 'Usuários', 'usuarios.sub': 'Comprador cadastra e cota; aprovador decide e recebe; admin faz tudo.', 'usuarios.novo': 'Novo usuário',
    'campo.sku': 'SKU', 'campo.nome': 'Nome', 'campo.unidade': 'Unid.', 'campo.saldo': 'Saldo', 'campo.minimo': 'Estoque mínimo',
    'campo.precoMedio': 'Custo médio', 'campo.produto': 'Produto', 'campo.tipo': 'Tipo', 'campo.quantidade': 'Quantidade',
    'campo.razaoSocial': 'Razão social', 'campo.documento': 'CPF/CNPJ', 'campo.email': 'E-mail', 'campo.prazoMedio': 'Prazo médio (dias)',
    'campo.status': 'Status', 'campo.codigo': 'Código', 'campo.propostas': 'Propostas', 'campo.dataLimite': 'Data limite', 'campo.numero': 'Número',
    'campo.cotacao': 'Cotação', 'campo.fornecedor': 'Fornecedor', 'campo.total': 'Valor total', 'campo.prazo': 'Prazo (dias)', 'campo.preco': 'Preço unit.',
    'campo.frete': 'Frete', 'campo.impostoPct': 'Imposto (%)', 'campo.impostoFixo': 'Imposto fixo', 'campo.validade': 'Validade', 'campo.imposto': 'Imposto',
    'campo.mercadoria': 'Mercadoria', 'campo.pontuacao': 'Pontuação', 'campo.criterio': 'Critério', 'campo.escolha': 'Proposta escolhida',
    'campo.justificativa': 'Justificativa', 'campo.convidados': 'Fornecedores convidados', 'campo.observacao': 'Observação', 'campo.itens': 'Itens',
    'campo.origem': 'Origem', 'campo.perfil': 'Perfil', 'campo.motivo': 'Motivo', 'campo.recebido': 'Recebido', 'campo.pendente': 'Pendente',
    'campo.custoEfetivo': 'Custo efetivo', 'campo.data': 'Data', 'campo.usuario': 'Usuário', 'campo.saldoApos': 'Saldo após', 'campo.referencia': 'Referência',
    'acao.cadastrar': 'Cadastrar', 'acao.movimentar': 'Registrar movimentação', 'acao.ativar': 'Ativar', 'acao.inativar': 'Inativar', 'acao.sair': 'Sair',
    'acao.abrir': 'Abrir cotação', 'acao.registrar': 'Registrar proposta', 'acao.decidir': 'Decidir e emitir pedido', 'acao.receber': 'Receber',
    'acao.fechar': 'Fechar', 'acao.cancelar': 'Cancelar', 'acao.confirmar': 'Confirmar', 'acao.extrato': 'Extrato', 'acao.editar': 'Editar',
    'acao.historico': 'Histórico', 'acao.salvar': 'Salvar', 'acao.criar': 'Criar cotação', 'acao.addItem': '+ Item', 'acao.remover': 'Remover',
    'acao.anterior': '‹ Anterior', 'acao.proxima': 'Próxima ›', 'acao.ver': 'Ver',
    'mov.SAIDA': 'Saída', 'mov.ENTRADA': 'Entrada', 'mov.AJUSTE': 'Ajuste (novo saldo)', 'mov.RECEBIMENTO': 'Recebimento', 'mov.SALDO_INICIAL': 'Saldo inicial',
    'filtro.todos': 'Todos', 'filtro.ativos': 'Ativos', 'filtro.inativos': 'Inativos', 'filtro.todas': 'Todas', 'filtro.buscar': 'Buscar…',
    'status.RASCUNHO': 'Rascunho', 'status.ABERTA': 'Aberta', 'status.FECHADA': 'Fechada', 'status.CANCELADA': 'Cancelada',
    'status.PENDENTE': 'Pendente', 'status.VENCEDORA': 'Vencedora', 'status.REJEITADA': 'Rejeitada', 'status.DESQUALIFICADA': 'Desqualificada',
    'status.EMITIDO': 'Emitido', 'status.PARCIALMENTE_RECEBIDO': 'Parcialmente recebido', 'status.RECEBIDO': 'Recebido', 'status.CANCELADO': 'Cancelado',
    'status.ativo': 'Ativo', 'status.inativo': 'Inativo', 'status.comprar': 'Comprar',
    'origem.AUTOMATICA': 'Reposição', 'origem.MANUAL': 'Manual',
    'crit.MENOR_PRECO': 'Menor preço', 'crit.PONTUACAO_PONDERADA': 'Pontuação ponderada (70% preço / 30% prazo)',
    'det.recomendacoes': 'Recomendação por critério', 'det.semRanking': 'Ainda não há propostas elegíveis.',
    'det.minimo': 'São necessárias {min} propostas válidas para decidir. Elegíveis agora: {n}.',
    'det.abrir': 'Abrir para propostas', 'det.mercado': 'Deixe todos desmarcados para abrir ao mercado.', 'det.proposta': 'Nova proposta',
    'det.decisao': 'Decisão', 'det.automatica': 'A recomendada pelo critério', 'det.justificativaDica': 'Obrigatória se a escolha for diferente da recomendada.',
    'det.aberta': 'Aberta ao mercado', 'det.dias': 'dias', 'det.pts': 'pts', 'det.vencedora': 'vencedora', 'det.decidida': 'Decidida por {u} em {d}',
    'det.cancelada': 'Cancelada por {u} em {d}: {m}', 'det.prazoEncerrado': 'Prazo encerrado: pronta para decisão', 'det.semPermissao': 'Seu perfil apenas consulta esta etapa.',
    'modal.cancelarCotacao': 'Cancelar cotação {c}', 'modal.cancelarPedido': 'Cancelar saldo do pedido {c}', 'modal.receber': 'Receber pedido {c}',
    'modal.receberDica': 'Informe o que chegou. Deixe zero nos itens que não vieram.', 'modal.extrato': 'Extrato de estoque · {c}',
    'modal.historico': 'Histórico · {c}', 'modal.editarProduto': 'Editar produto {c}', 'modal.novaCotacao': 'Nova cotação', 'modal.pedido': 'Pedido {c}',
    'ok.produto': 'Produto cadastrado.', 'ok.produtoCotacao': 'Produto cadastrado. Estoque no mínimo: incluído na reposição.', 'ok.movimento': 'Estoque atualizado.',
    'ok.movimentoCotacao': 'Estoque atualizado. Produto no mínimo: confira a aba Cotações.', 'ok.salvo': 'Alterações salvas.',
    'ok.fornecedor': 'Fornecedor cadastrado.', 'ok.status': 'Status atualizado.', 'ok.aberta': 'Cotação aberta; convites enviados.',
    'ok.proposta': 'Proposta registrada: total {total}.', 'ok.decisao': 'Pedido {pedido} emitido para {fornecedor}', 'ok.cotacao': 'Cotação {c} criada.',
    'ok.recebido': 'Recebimento do pedido {pedido} registrado.', 'ok.cancelado': 'Cancelamento registrado.', 'ok.usuario': 'Usuário criado.',
    'paginacao': 'Página {p} de {t} · {n} registros', 'vazio': 'Nada por aqui ainda.', 'erro.rede': 'Não foi possível falar com a API.',
    'erro.sessao': 'Sua sessão expirou. Entre novamente.'
  },
  en: {
    'login.sub': 'Sign in to access the system.', 'login.usuario': 'Username', 'login.senha': 'Password', 'login.entrar': 'Sign in',
    'login.demo': 'Demo: comprador / comprador123 · aprovador / aprovador123 · admin / admin123456',
    'aba.produtos': 'Products', 'aba.fornecedores': 'Suppliers', 'aba.cotacoes': 'Quotations', 'aba.pedidos': 'Purchase orders', 'aba.usuarios': 'Users',
    'kpi.necessidade': 'Products at minimum stock', 'kpi.rascunhos': 'Draft quotations', 'kpi.abertas': 'Open quotations', 'kpi.emitidos': 'Orders to receive',
    'produtos.titulo': 'Products and stock', 'produtos.sub': 'When the balance reaches the minimum stock, the product is added to the replenishment draft automatically.',
    'produtos.novo': 'New product', 'produtos.movimentar': 'Stock movement', 'produtos.filtro': 'Only items that need purchasing',
    'fornecedores.titulo': 'Suppliers', 'fornecedores.sub': 'Proposals from inactive suppliers no longer count in the decision.', 'fornecedores.novo': 'New supplier',
    'cotacoes.titulo': 'Quotations', 'cotacoes.sub': 'Click a quotation to open it, register proposals and pick the winner.', 'cotacoes.nova': '+ New quotation',
    'pedidos.titulo': 'Purchase orders', 'pedidos.sub': 'Created by the decision. Receiving (full or partial) adds to stock and recalculates the average cost.',
    'usuarios.titulo': 'Users', 'usuarios.sub': 'Buyers register and quote; approvers decide and receive; admins do everything.', 'usuarios.novo': 'New user',
    'campo.sku': 'SKU', 'campo.nome': 'Name', 'campo.unidade': 'Unit', 'campo.saldo': 'Balance', 'campo.minimo': 'Minimum stock',
    'campo.precoMedio': 'Average cost', 'campo.produto': 'Product', 'campo.tipo': 'Type', 'campo.quantidade': 'Quantity',
    'campo.razaoSocial': 'Company name', 'campo.documento': 'Tax ID (CPF/CNPJ)', 'campo.email': 'E-mail', 'campo.prazoMedio': 'Average lead time (days)',
    'campo.status': 'Status', 'campo.codigo': 'Code', 'campo.propostas': 'Proposals', 'campo.dataLimite': 'Deadline', 'campo.numero': 'Number',
    'campo.cotacao': 'Quotation', 'campo.fornecedor': 'Supplier', 'campo.total': 'Total', 'campo.prazo': 'Lead time (days)', 'campo.preco': 'Unit price',
    'campo.frete': 'Freight', 'campo.impostoPct': 'Tax (%)', 'campo.impostoFixo': 'Fixed tax', 'campo.validade': 'Valid until', 'campo.imposto': 'Tax',
    'campo.mercadoria': 'Goods', 'campo.pontuacao': 'Score', 'campo.criterio': 'Criterion', 'campo.escolha': 'Chosen proposal',
    'campo.justificativa': 'Justification', 'campo.convidados': 'Invited suppliers', 'campo.observacao': 'Notes', 'campo.itens': 'Items',
    'campo.origem': 'Origin', 'campo.perfil': 'Profile', 'campo.motivo': 'Reason', 'campo.recebido': 'Received', 'campo.pendente': 'Pending',
    'campo.custoEfetivo': 'Effective cost', 'campo.data': 'Date', 'campo.usuario': 'User', 'campo.saldoApos': 'Balance after', 'campo.referencia': 'Reference',
    'acao.cadastrar': 'Create', 'acao.movimentar': 'Record movement', 'acao.ativar': 'Activate', 'acao.inativar': 'Deactivate', 'acao.sair': 'Sign out',
    'acao.abrir': 'Open quotation', 'acao.registrar': 'Register proposal', 'acao.decidir': 'Decide and issue order', 'acao.receber': 'Receive',
    'acao.fechar': 'Close', 'acao.cancelar': 'Cancel', 'acao.confirmar': 'Confirm', 'acao.extrato': 'Ledger', 'acao.editar': 'Edit',
    'acao.historico': 'History', 'acao.salvar': 'Save', 'acao.criar': 'Create quotation', 'acao.addItem': '+ Item', 'acao.remover': 'Remove',
    'acao.anterior': '‹ Previous', 'acao.proxima': 'Next ›', 'acao.ver': 'View',
    'mov.SAIDA': 'Withdrawal', 'mov.ENTRADA': 'Receipt', 'mov.AJUSTE': 'Adjustment (new balance)', 'mov.RECEBIMENTO': 'Order receipt', 'mov.SALDO_INICIAL': 'Opening balance',
    'filtro.todos': 'All', 'filtro.ativos': 'Active', 'filtro.inativos': 'Inactive', 'filtro.todas': 'All', 'filtro.buscar': 'Search…',
    'status.RASCUNHO': 'Draft', 'status.ABERTA': 'Open', 'status.FECHADA': 'Closed', 'status.CANCELADA': 'Cancelled',
    'status.PENDENTE': 'Pending', 'status.VENCEDORA': 'Winner', 'status.REJEITADA': 'Rejected', 'status.DESQUALIFICADA': 'Disqualified',
    'status.EMITIDO': 'Issued', 'status.PARCIALMENTE_RECEBIDO': 'Partially received', 'status.RECEBIDO': 'Received', 'status.CANCELADO': 'Cancelled',
    'status.ativo': 'Active', 'status.inativo': 'Inactive', 'status.comprar': 'Reorder',
    'origem.AUTOMATICA': 'Replenishment', 'origem.MANUAL': 'Manual',
    'crit.MENOR_PRECO': 'Lowest price', 'crit.PONTUACAO_PONDERADA': 'Weighted score (70% price / 30% lead time)',
    'det.recomendacoes': 'Recommendation by criterion', 'det.semRanking': 'No eligible proposals yet.',
    'det.minimo': '{min} valid proposals are required to decide. Eligible now: {n}.',
    'det.abrir': 'Open for proposals', 'det.mercado': 'Leave all unchecked to open it to the market.', 'det.proposta': 'New proposal',
    'det.decisao': 'Decision', 'det.automatica': 'The one recommended by the criterion', 'det.justificativaDica': 'Required when the choice differs from the recommendation.',
    'det.aberta': 'Open to the market', 'det.dias': 'days', 'det.pts': 'pts', 'det.vencedora': 'winner', 'det.decidida': 'Decided by {u} on {d}',
    'det.cancelada': 'Cancelled by {u} on {d}: {m}', 'det.prazoEncerrado': 'Deadline passed: ready for decision', 'det.semPermissao': 'Your profile can only view this step.',
    'modal.cancelarCotacao': 'Cancel quotation {c}', 'modal.cancelarPedido': 'Cancel remaining items of order {c}', 'modal.receber': 'Receive order {c}',
    'modal.receberDica': 'Enter what arrived. Leave zero for items that did not come.', 'modal.extrato': 'Stock ledger · {c}',
    'modal.historico': 'History · {c}', 'modal.editarProduto': 'Edit product {c}', 'modal.novaCotacao': 'New quotation', 'modal.pedido': 'Order {c}',
    'ok.produto': 'Product created.', 'ok.produtoCotacao': 'Product created. Stock at minimum: added to replenishment.', 'ok.movimento': 'Stock updated.',
    'ok.movimentoCotacao': 'Stock updated. Product at minimum: check the Quotations tab.', 'ok.salvo': 'Changes saved.',
    'ok.fornecedor': 'Supplier created.', 'ok.status': 'Status updated.', 'ok.aberta': 'Quotation opened; invitations sent.',
    'ok.proposta': 'Proposal registered: total {total}.', 'ok.decisao': 'Order {pedido} issued to {fornecedor}', 'ok.cotacao': 'Quotation {c} created.',
    'ok.recebido': 'Receipt for order {pedido} recorded.', 'ok.cancelado': 'Cancellation recorded.', 'ok.usuario': 'User created.',
    'paginacao': 'Page {p} of {t} · {n} records', 'vazio': 'Nothing here yet.', 'erro.rede': 'Could not reach the API.',
    'erro.sessao': 'Your session has expired. Please sign in again.'
  }
};

const ABAS = ['produtos', 'fornecedores', 'cotacoes', 'pedidos', 'usuarios'];

const estado = {
  lang: (() => { const s = localStorage.getItem('lang'); return s === 'pt' || s === 'en' ? s : ((navigator.language || 'pt').toLowerCase().startsWith('en') ? 'en' : 'pt'); })(),
  token: sessionStorage.getItem('token'),
  usuario: null,
  aba: location.hash.replace('#', '') || 'produtos',
  pagina: { produtos: 0, fornecedores: 0, cotacoes: 0, pedidos: 0 },
  cotacaoAberta: null
};

/* ---------------- utilitários ---------------- */
const $ = (s, el = document) => el.querySelector(s);
const t = (k, vars = {}) => (I18N[estado.lang][k] ?? k).replace(/\{(\w+)\}/g, (_, v) => vars[v] ?? '');
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const locale = () => estado.lang === 'en' ? 'en-US' : 'pt-BR';
const moeda = v => v == null ? '–' : new Intl.NumberFormat(locale(), { style: 'currency', currency: 'BRL' }).format(v);
const num = (v, max = 4) => v == null ? '–' : new Intl.NumberFormat(locale(), { maximumFractionDigits: max }).format(v);
const data = v => v ? new Intl.DateTimeFormat(locale()).format(new Date(v.length === 10 ? v + 'T00:00:00' : v)) : '–';
const dataHora = v => v ? new Intl.DateTimeFormat(locale(), { dateStyle: 'short', timeStyle: 'short' }).format(new Date(v)) : '–';
const tag = (status, texto) => `<span class="tag t-${esc(status)}">${esc(texto ?? t('status.' + status))}</span>`;
const hojeISO = (dias = 0) => { const d = new Date(); d.setDate(d.getDate() + dias); return d.toLocaleDateString('sv-SE'); };
const pode = perfil => !!estado.usuario && (estado.usuario.perfil === 'ADMIN' || estado.usuario.perfil === perfil);
const resumoItens = itens => itens.map(i => `<b>${esc(i.produtoSku)}</b> × ${num(i.quantidade)}`).join(' · ');

async function api(metodo, caminho, corpo) {
  const headers = { 'Accept-Language': estado.lang === 'en' ? 'en' : 'pt-BR' };
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (estado.token) headers.Authorization = 'Bearer ' + estado.token;
  let resposta;
  try {
    resposta = await fetch(caminho, { method: metodo, headers, body: corpo === undefined ? undefined : JSON.stringify(corpo) });
  } catch (e) { throw { mensagem: t('erro.rede') }; }
  const texto = await resposta.text();
  const json = texto ? JSON.parse(texto) : null;
  if (resposta.status === 401 && caminho !== '/api/auth/login') {
    sair(true);
    throw { mensagem: t('erro.sessao') };
  }
  if (!resposta.ok) throw json || { mensagem: resposta.statusText };
  return json;
}

function avisar(texto, erro = false, campos = []) {
  const el = document.createElement('div');
  el.className = 'msg' + (erro ? ' erro' : '');
  el.innerHTML = esc(texto) + (campos.length ? '<ul>' + campos.map(c => `<li><b>${esc(c.campo)}</b>: ${esc(c.mensagem)}</li>`).join('') + '</ul>' : '');
  $('#toast').append(el);
  setTimeout(() => el.remove(), erro ? 7000 : 4000);
}
const falhou = e => avisar(e.mensagem || String(e), true, e.campos || []);

function dadosDoForm(form) {
  const obj = {};
  for (const [k, v] of new FormData(form).entries()) obj[k] = v === '' ? null : v;
  return obj;
}

function paginador(id, pagina, aoMudar) {
  const el = $('#' + id);
  if (!pagina || pagina.totalPaginas <= 1) { el.innerHTML = pagina && pagina.totalElementos ? t('paginacao', { p: 1, t: 1, n: pagina.totalElementos }) : ''; return; }
  el.innerHTML = `<span>${t('paginacao', { p: pagina.pagina + 1, t: pagina.totalPaginas, n: pagina.totalElementos })}</span>
    <button class="secundario" data-p="${pagina.pagina - 1}" ${pagina.pagina === 0 ? 'disabled' : ''}>${t('acao.anterior')}</button>
    <button class="secundario" data-p="${pagina.pagina + 1}" ${pagina.pagina + 1 >= pagina.totalPaginas ? 'disabled' : ''}>${t('acao.proxima')}</button>`;
  el.querySelectorAll('[data-p]').forEach(b => b.onclick = () => aoMudar(Number(b.dataset.p)));
}

/* ---------------- modal ---------------- */
function abrirModal(html, aoEnviar) {
  const dialog = $('#modal');
  $('#modal-conteudo').innerHTML = html;
  const form = $('#modal-conteudo form');
  if (form && aoEnviar) {
    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      try { await aoEnviar(form); dialog.close(); } catch (e) { falhou(e); }
    });
  }
  $('#modal-conteudo').querySelectorAll('[data-fechar]').forEach(b => b.onclick = () => dialog.close());
  dialog.showModal();
}

function pedirMotivo(titulo, aoConfirmar) {
  abrirModal(`<h3>${esc(titulo)}</h3><form>
      <label>${t('campo.motivo')}<textarea name="motivo" rows="3" maxlength="500" required></textarea></label>
      <div class="modal-acoes"><button type="button" class="secundario" data-fechar>${t('acao.fechar')}</button>
      <button class="primario">${t('acao.confirmar')}</button></div></form>`,
    async form => aoConfirmar(form.motivo.value.trim()));
}

function mostrarHistorico(titulo, eventos) {
  const itens = eventos.length ? eventos.map(e => `<li><b>${esc(e.descricao)}</b>${e.detalhe ? ' · ' + esc(e.detalhe) : ''}
      <small>${dataHora(e.criadoEm)} · ${esc(e.usuario)}</small></li>`).join('') : `<li>${t('vazio')}</li>`;
  abrirModal(`<h3>${esc(titulo)}</h3><ul class="historico">${itens}</ul>
      <div class="modal-acoes"><button class="secundario" data-fechar>${t('acao.fechar')}</button></div>`);
}

/* ---------------- sessão ---------------- */
function aplicarIdioma() {
  document.documentElement.lang = estado.lang === 'en' ? 'en' : 'pt-BR';
  document.querySelectorAll('[data-i18n]').forEach(el => el.textContent = t(el.dataset.i18n));
  document.querySelectorAll('[data-i18n-ph]').forEach(el => el.placeholder = t(el.dataset.i18nPh));
  document.querySelectorAll('[data-lang]').forEach(b => b.classList.toggle('ativo', b.dataset.lang === estado.lang));
}

function aplicarPerfil() {
  document.querySelectorAll('[data-perfil]').forEach(el => el.hidden = !pode(el.dataset.perfil));
  $('#usuario-nome').textContent = estado.usuario.nome;
  $('#usuario-perfil').textContent = estado.usuario.perfil;
}

function mostrarLogin() {
  $('#tela-app').hidden = true;
  $('#tela-login').hidden = false;
  $('#form-login').username.focus();
}

function sair(expirou = false) {
  estado.token = null;
  estado.usuario = null;
  sessionStorage.removeItem('token');
  if (expirou) avisar(t('erro.sessao'), true);
  mostrarLogin();
}

function entrarNoApp() {
  $('#tela-login').hidden = true;
  $('#tela-app').hidden = false;
  aplicarPerfil();
  mostrarAba(ABAS.includes(estado.aba) && (estado.aba !== 'usuarios' || pode('ADMIN')) ? estado.aba : 'produtos');
}

$('#form-login').addEventListener('submit', async ev => {
  ev.preventDefault();
  try {
    const r = await api('POST', '/api/auth/login', dadosDoForm(ev.target));
    estado.token = r.token;
    estado.usuario = r.usuario;
    sessionStorage.setItem('token', r.token);
    ev.target.reset();
    entrarNoApp();
  } catch (e) { falhou(e); }
});
$('#sair').addEventListener('click', () => sair());

/* ---------------- navegação ---------------- */
function mostrarAba(aba) {
  estado.aba = aba;
  history.replaceState(null, '', '#' + aba);
  document.querySelectorAll('[data-view]').forEach(s => s.hidden = s.dataset.view !== aba);
  document.querySelectorAll('[data-aba]').forEach(b => b.classList.toggle('ativo', b.dataset.aba === aba));
  recarregar();
}
const carregadores = () => ({ produtos: carregarProdutos, fornecedores: carregarFornecedores, cotacoes: carregarCotacoes,
  pedidos: carregarPedidos, usuarios: carregarUsuarios });
async function recarregar() {
  if (!estado.usuario) return;
  try {
    await Promise.all([carregarKpis(), carregadores()[estado.aba]()]);
    if (estado.aba === 'cotacoes' && estado.cotacaoAberta) await abrirDetalhe(estado.cotacaoAberta);
  } catch (e) { falhou(e); }
}

async function carregarKpis() {
  const [necessidade, rascunhos, abertas, emitidos, parciais] = await Promise.all([
    api('GET', '/api/produtos?apenasNecessidadeCompra=true&ativo=true&tamanho=1'), api('GET', '/api/cotacoes?status=RASCUNHO&tamanho=1'),
    api('GET', '/api/cotacoes?status=ABERTA&tamanho=1'), api('GET', '/api/pedidos-compra?status=EMITIDO&tamanho=1'),
    api('GET', '/api/pedidos-compra?status=PARCIALMENTE_RECEBIDO&tamanho=1')]);
  $('#kpi-necessidade').textContent = necessidade.totalElementos;
  $('#kpi-rascunhos').textContent = rascunhos.totalElementos;
  $('#kpi-abertas').textContent = abertas.totalElementos;
  $('#kpi-emitidos').textContent = emitidos.totalElementos + parciais.totalElementos;
}

/* ---------------- produtos ---------------- */
async function carregarProdutos() {
  const params = new URLSearchParams({ pagina: estado.pagina.produtos, tamanho: 15 });
  if ($('#filtro-necessidade').checked) params.set('apenasNecessidadeCompra', 'true');
  if ($('#busca-produto').value.trim()) params.set('busca', $('#busca-produto').value.trim());
  const [pagina, ativos] = await Promise.all([api('GET', '/api/produtos?' + params), api('GET', '/api/produtos?ativo=true&tamanho=100')]);
  const edita = pode('COMPRADOR');
  $('#tb-produtos').innerHTML = pagina.conteudo.length ? pagina.conteudo.map(p => `<tr>
      <td class="mono">${esc(p.sku)}</td><td>${esc(p.nome)}${p.ativo ? '' : ' ' + tag('off', t('status.inativo'))}</td><td>${esc(p.unidade)}</td>
      <td class="num">${num(p.saldoAtual)}</td><td class="num">${num(p.estoqueMinimo)}</td><td class="num">${moeda(p.precoMedioCompra)}</td>
      <td>${p.necessitaCompra && p.ativo ? tag('alerta', t('status.comprar')) : ''}</td>
      <td class="acoes"><button class="secundario" data-extrato="${p.id}" data-sku="${esc(p.sku)}">${t('acao.extrato')}</button>
        ${edita ? `<button class="secundario" data-editar="${p.id}">${t('acao.editar')}</button>
        <button class="secundario" data-ativo-produto="${p.id}" data-ativo="${!p.ativo}">${t(p.ativo ? 'acao.inativar' : 'acao.ativar')}</button>` : ''}</td></tr>`).join('')
    : `<tr><td colspan="8" class="vazio">${t('vazio')}</td></tr>`;
  paginador('pg-produtos', pagina, p => { estado.pagina.produtos = p; carregarProdutos().catch(falhou); });
  const sel = $('#sel-produto-estoque'), atual = sel.value;
  sel.innerHTML = ativos.conteudo.map(p => `<option value="${p.id}">${esc(p.sku)} · ${esc(p.nome)}</option>`).join('');
  if (atual) sel.value = atual;
}

$('#tb-produtos').addEventListener('click', async ev => {
  const b = ev.target.closest('button');
  if (!b) return;
  try {
    if (b.dataset.extrato) {
      const ext = await api('GET', `/api/produtos/${b.dataset.extrato}/movimentacoes?tamanho=50`);
      const linhas = ext.conteudo.map(m => `<tr><td>${dataHora(m.criadoEm)}</td><td>${t('mov.' + m.tipo)}</td>
          <td class="num">${num(m.quantidade)}</td><td class="num">${num(m.saldoPosterior)}</td><td class="num">${moeda(m.custoUnitario)}</td>
          <td>${esc(m.referencia || m.observacao || '')}</td><td>${esc(m.usuario)}</td></tr>`).join('');
      abrirModal(`<h3>${t('modal.extrato', { c: b.dataset.sku })}</h3><table class="subtabela"><thead><tr><th>${t('campo.data')}</th>
          <th>${t('campo.tipo')}</th><th class="num">${t('campo.quantidade')}</th><th class="num">${t('campo.saldoApos')}</th>
          <th class="num">${t('campo.custoEfetivo')}</th><th>${t('campo.referencia')}</th><th>${t('campo.usuario')}</th></tr></thead>
          <tbody>${linhas || `<tr><td colspan="7" class="vazio">${t('vazio')}</td></tr>`}</tbody></table>
          <div class="modal-acoes"><button class="secundario" data-fechar>${t('acao.fechar')}</button></div>`);
    } else if (b.dataset.editar) {
      const p = await api('GET', `/api/produtos/${b.dataset.editar}`);
      abrirModal(`<h3>${t('modal.editarProduto', { c: esc(p.sku) })}</h3><form>
          <label>${t('campo.nome')}<input name="nome" required maxlength="150" value="${esc(p.nome)}"></label>
          <div class="linha"><label>${t('campo.unidade')}<select name="unidade">${['UN', 'CX', 'KG', 'PC', 'LT', 'MT'].map(u =>
            `<option ${u === p.unidade ? 'selected' : ''}>${u}</option>`).join('')}</select></label>
          <label>${t('campo.minimo')}<input name="estoqueMinimo" type="number" step="0.0001" min="0" required value="${p.estoqueMinimo}"></label></div>
          <div class="modal-acoes"><button type="button" class="secundario" data-fechar>${t('acao.fechar')}</button><button class="primario">${t('acao.salvar')}</button></div></form>`,
        async form => { await api('PUT', `/api/produtos/${p.id}`, dadosDoForm(form)); avisar(t('ok.salvo')); recarregar(); });
    } else if (b.dataset.ativoProduto) {
      await api('PATCH', `/api/produtos/${b.dataset.ativoProduto}/status`, { ativo: b.dataset.ativo === 'true' });
      avisar(t('ok.status'));
      recarregar();
    }
  } catch (e) { falhou(e); }
});

$('#form-produto').addEventListener('submit', async ev => {
  ev.preventDefault();
  try {
    const p = await api('POST', '/api/produtos', dadosDoForm(ev.target));
    avisar(t(p.necessitaCompra ? 'ok.produtoCotacao' : 'ok.produto'));
    ev.target.reset();
    recarregar();
  } catch (e) { falhou(e); }
});
$('#form-estoque').addEventListener('submit', async ev => {
  ev.preventDefault();
  const { produtoId, ...corpo } = dadosDoForm(ev.target);
  try {
    const p = await api('PATCH', `/api/produtos/${produtoId}/estoque`, corpo);
    avisar(t(p.necessitaCompra && corpo.tipo !== 'ENTRADA' ? 'ok.movimentoCotacao' : 'ok.movimento'));
    ev.target.quantidade.value = '';
    ev.target.observacao.value = '';
    recarregar();
  } catch (e) { falhou(e); }
});
let esperaBusca;
const buscarDepois = (aba, fn) => () => { clearTimeout(esperaBusca); esperaBusca = setTimeout(() => { estado.pagina[aba] = 0; fn().catch(falhou); }, 300); };
$('#busca-produto').addEventListener('input', buscarDepois('produtos', carregarProdutos));
$('#filtro-necessidade').addEventListener('change', () => { estado.pagina.produtos = 0; carregarProdutos().catch(falhou); });

/* ---------------- fornecedores ---------------- */
async function carregarFornecedores() {
  const params = new URLSearchParams({ pagina: estado.pagina.fornecedores, tamanho: 15 });
  if ($('#filtro-fornecedor').value) params.set('ativo', $('#filtro-fornecedor').value);
  if ($('#busca-fornecedor').value.trim()) params.set('busca', $('#busca-fornecedor').value.trim());
  const pagina = await api('GET', '/api/fornecedores?' + params);
  const edita = pode('COMPRADOR');
  $('#tb-fornecedores').innerHTML = pagina.conteudo.length ? pagina.conteudo.map(f => `<tr>
      <td>${esc(f.razaoSocial)}</td><td class="mono">${esc(f.documento)}</td><td>${esc(f.email)}</td>
      <td class="num">${f.prazoMedioDias}</td><td>${f.ativo ? tag('ok', t('status.ativo')) : tag('off', t('status.inativo'))}</td>
      <td class="acoes">${edita ? `<button class="secundario" data-status="${f.id}" data-ativo="${!f.ativo}">${t(f.ativo ? 'acao.inativar' : 'acao.ativar')}</button>` : ''}</td></tr>`).join('')
    : `<tr><td colspan="6" class="vazio">${t('vazio')}</td></tr>`;
  paginador('pg-fornecedores', pagina, p => { estado.pagina.fornecedores = p; carregarFornecedores().catch(falhou); });
}
$('#tb-fornecedores').addEventListener('click', async ev => {
  const b = ev.target.closest('[data-status]');
  if (!b) return;
  try {
    await api('PATCH', `/api/fornecedores/${b.dataset.status}/status`, { ativo: b.dataset.ativo === 'true' });
    avisar(t('ok.status'));
    carregarFornecedores();
  } catch (e) { falhou(e); }
});
$('#form-fornecedor').addEventListener('submit', async ev => {
  ev.preventDefault();
  try {
    await api('POST', '/api/fornecedores', dadosDoForm(ev.target));
    avisar(t('ok.fornecedor'));
    ev.target.reset();
    carregarFornecedores();
  } catch (e) { falhou(e); }
});
$('#filtro-fornecedor').addEventListener('change', () => { estado.pagina.fornecedores = 0; carregarFornecedores().catch(falhou); });
$('#busca-fornecedor').addEventListener('input', buscarDepois('fornecedores', carregarFornecedores));

/* ---------------- cotações ---------------- */
async function carregarCotacoes() {
  const params = new URLSearchParams({ pagina: estado.pagina.cotacoes, tamanho: 15 });
  if ($('#filtro-cotacao').value) params.set('status', $('#filtro-cotacao').value);
  const pagina = await api('GET', '/api/cotacoes?' + params);
  $('#tb-cotacoes').innerHTML = pagina.conteudo.length ? pagina.conteudo.map(c => `<tr class="clicavel" data-cotacao="${c.id}">
      <td class="mono">${esc(c.codigo)}</td><td class="itens-celula">${resumoItens(c.itens)}</td><td>${t('origem.' + c.origem)}</td>
      <td>${tag(c.status)}</td><td class="num">${c.quantidadePropostas}</td><td>${data(c.dataLimite)}</td></tr>`).join('')
    : `<tr><td colspan="6" class="vazio">${t('vazio')}</td></tr>`;
  paginador('pg-cotacoes', pagina, p => { estado.pagina.cotacoes = p; carregarCotacoes().catch(falhou); });
}
$('#tb-cotacoes').addEventListener('click', ev => {
  const linha = ev.target.closest('[data-cotacao]');
  if (linha) abrirDetalhe(Number(linha.dataset.cotacao)).then(() => $('#detalhe').scrollIntoView({ behavior: 'smooth' })).catch(falhou);
});
$('#filtro-cotacao').addEventListener('change', () => { estado.pagina.cotacoes = 0; carregarCotacoes().catch(falhou); });

$('#nova-cotacao').addEventListener('click', async () => {
  try {
    const produtos = (await api('GET', '/api/produtos?ativo=true&tamanho=100')).conteudo;
    const opcoes = produtos.map(p => `<option value="${p.id}">${esc(p.sku)} · ${esc(p.nome)}</option>`).join('');
    const linha = () => `<div class="linha-item"><label>${t('campo.produto')}<select name="produtoId">${opcoes}</select></label>
        <label>${t('campo.quantidade')}<input name="quantidade" type="number" step="0.0001" min="0.0001" required></label>
        <button type="button" class="secundario" data-remover>${t('acao.remover')}</button></div>`;
    abrirModal(`<h3>${t('modal.novaCotacao')}</h3><form><div id="itens-nova">${linha()}</div>
        <button type="button" class="secundario" id="add-item">${t('acao.addItem')}</button>
        <div class="linha"><label>${t('campo.status')}<select name="status"><option value="RASCUNHO">${t('status.RASCUNHO')}</option>
          <option value="ABERTA">${t('status.ABERTA')}</option></select></label>
        <label>${t('campo.dataLimite')}<input type="date" name="dataLimite" min="${hojeISO()}"></label></div>
        <label>${t('campo.observacao')}<input name="observacao" maxlength="500"></label>
        <div class="modal-acoes"><button type="button" class="secundario" data-fechar>${t('acao.fechar')}</button><button class="primario">${t('acao.criar')}</button></div></form>`,
      async form => {
        const itens = [...form.querySelectorAll('.linha-item')].map(l => ({ produtoId: Number(l.querySelector('select').value), quantidade: l.querySelector('input').value }));
        const c = await api('POST', '/api/cotacoes', { itens, status: form.status.value, dataLimite: form.dataLimite.value || null, observacao: form.observacao.value || null });
        avisar(t('ok.cotacao', { c: c.codigo }));
        estado.cotacaoAberta = c.id;
        recarregar();
      });
    const area = $('#itens-nova');
    $('#add-item').onclick = () => area.insertAdjacentHTML('beforeend', linha());
    area.addEventListener('click', ev => { const r = ev.target.closest('[data-remover]'); if (r && area.children.length > 1) r.parentElement.remove(); });
  } catch (e) { falhou(e); }
});

function blocoRecomendacao(rec, vencedoraId) {
  const itens = rec.ranking.map(i => `<li>${esc(i.fornecedorRazaoSocial)} — ${moeda(i.valorTotal)}
      <span>· ${i.prazoEntregaDias} ${t('det.dias')} · ${num(i.pontuacao, 2)} ${t('det.pts')}${i.propostaId === vencedoraId ? ' · ' + t('det.vencedora') : ''}</span></li>`).join('');
  return `<div class="rec"><h4>${t('crit.' + rec.criterio)}</h4>${itens ? `<ol>${itens}</ol>` : `<span class="sub">${t('det.semRanking')}</span>`}</div>`;
}

async function abrirDetalhe(id) {
  estado.cotacaoAberta = id;
  const c = await api('GET', `/api/cotacoes/${id}`);
  const fornecedores = c.status === 'RASCUNHO' || c.status === 'ABERTA' ? (await api('GET', '/api/fornecedores?ativo=true&tamanho=100')).conteudo : [];
  const el = $('#detalhe');
  el.hidden = false;
  const andamento = c.status === 'RASCUNHO' || c.status === 'ABERTA';
  const comprador = pode('COMPRADOR'), aprovador = pode('APROVADOR');

  const itens = `<table class="subtabela"><thead><tr><th>${t('campo.produto')}</th><th class="num">${t('campo.quantidade')}</th>
      <th class="num">${t('campo.saldo')}</th><th class="num">${t('campo.minimo')}</th></tr></thead><tbody>${c.itens.map(i => `<tr>
      <td><span class="mono">${esc(i.produtoSku)}</span> · ${esc(i.produtoNome)}</td><td class="num">${num(i.quantidade)} ${esc(i.unidade)}</td>
      <td class="num">${num(i.saldoAtual)}</td><td class="num">${num(i.estoqueMinimo)}</td></tr>`).join('')}</tbody></table>`;

  const propostas = c.propostas.length ? `<h4 class="titulo-recs">${t('campo.propostas')}</h4><table><thead><tr>
      <th>${t('campo.fornecedor')}</th><th>${t('campo.itens')}</th><th class="num">${t('campo.mercadoria')}</th><th class="num">${t('campo.frete')}</th>
      <th class="num">${t('campo.imposto')}</th><th class="num">${t('campo.total')}</th><th class="num">${t('campo.prazo')}</th>
      <th>${t('campo.validade')}</th><th class="num">${t('campo.pontuacao')}</th><th>${t('campo.status')}</th></tr></thead><tbody>${c.propostas.map(p => `<tr>
      <td>${esc(p.fornecedorRazaoSocial)}${p.fornecedorAtivo ? '' : ' ' + tag('off', t('status.inativo'))}</td>
      <td class="itens-celula">${p.itens.map(i => `<b>${esc(i.produtoSku)}</b> ${moeda(i.precoUnitario)}`).join(' · ')}</td>
      <td class="num">${moeda(p.valorMercadoria)}</td><td class="num">${moeda(p.valorFrete)}</td><td class="num">${moeda(p.valorImpostoCalculado)}</td>
      <td class="num"><b>${moeda(p.valorTotal)}</b></td><td class="num">${p.prazoEntregaDias}</td><td>${data(p.dataValidade)}</td>
      <td class="num">${num(p.pontuacaoCalculada, 2)}</td><td>${tag(p.status)}</td></tr>`).join('')}</tbody></table>` : '';

  const painelAbrir = c.status === 'RASCUNHO' && comprador ? `<div class="card"><h3>${t('det.abrir')}</h3><form id="form-abrir">
      <label>${t('campo.convidados')}<div class="checks">${fornecedores.map(f =>
        `<label><input type="checkbox" name="forn" value="${f.id}"> ${esc(f.razaoSocial)}</label>`).join('')}</div><span>${t('det.mercado')}</span></label>
      <label>${t('campo.dataLimite')}<input type="date" name="dataLimite" min="${hojeISO()}" value="${hojeISO(7)}"></label>
      <button class="primario">${t('acao.abrir')}</button></form></div>` : '';

  const convidados = new Set(c.fornecedoresConvidados.map(f => f.id));
  const podemEnviar = c.abertaAoMercado ? fornecedores : fornecedores.filter(f => convidados.has(f.id));
  const painelProposta = c.status === 'ABERTA' && comprador ? `<div class="card"><h3>${t('det.proposta')}</h3><form id="form-proposta">
      <label>${t('campo.fornecedor')}<select name="fornecedorId" required>${podemEnviar.map(f => `<option value="${f.id}">${esc(f.razaoSocial)}</option>`).join('')}</select></label>
      ${c.itens.map(i => `<label><span>${t('campo.preco')} · <span class="mono">${esc(i.produtoSku)}</span> (${num(i.quantidade)} ${esc(i.unidade)})</span>
        <input name="preco-${i.produtoId}" data-produto="${i.produtoId}" type="number" step="0.0001" min="0.0001" required></label>`).join('')}
      <div class="linha"><label>${t('campo.frete')}<input name="valorFrete" type="number" step="0.01" min="0" value="0"></label>
        <label>${t('campo.impostoPct')}<input name="impostoPercentual" type="number" step="0.0001" min="0" max="100" value="0"></label></div>
      <div class="linha"><label>${t('campo.impostoFixo')}<input name="valorImpostoFixo" type="number" step="0.01" min="0" value="0"></label>
        <label>${t('campo.prazo')}<input name="prazoEntregaDias" type="number" min="0" max="3650" required></label></div>
      <label>${t('campo.validade')}<input name="dataValidade" type="date" min="${hojeISO()}" value="${hojeISO(30)}" required></label>
      <button class="primario">${t('acao.registrar')}</button></form></div>` : '';

  const elegiveis = c.propostas.filter(p => p.status === 'PENDENTE' && p.fornecedorAtivo);
  const painelDecisao = c.status === 'ABERTA' && aprovador ? `<div class="card"><h3>${t('det.decisao')}</h3>
      ${c.podeDecidir ? '' : `<div class="aviso">${t('det.minimo', { min: c.minimoPropostas, n: c.propostasElegiveis })}</div>`}
      <form id="form-decidir">
      <label>${t('campo.criterio')}<select name="criterio"><option value="MENOR_PRECO">${t('crit.MENOR_PRECO')}</option>
        <option value="PONTUACAO_PONDERADA">${t('crit.PONTUACAO_PONDERADA')}</option></select></label>
      <label>${t('campo.escolha')}<select name="propostaIdEscolhida"><option value="">${t('det.automatica')}</option>
        ${elegiveis.map(p => `<option value="${p.id}">${esc(p.fornecedorRazaoSocial)} — ${moeda(p.valorTotal)}</option>`).join('')}</select></label>
      <label>${t('campo.justificativa')}<textarea name="justificativaEscolha" rows="2" maxlength="500" placeholder="${t('det.justificativaDica')}"></textarea></label>
      <button class="primario" ${c.podeDecidir ? '' : 'disabled'}>${t('acao.decidir')}</button></form></div>` : '';

  const situacao = c.status === 'FECHADA' && c.decididoPor ? `<div class="meta"><span>${t('det.decidida', { u: esc(c.decididoPor), d: dataHora(c.decididoEm) })}</span></div>`
    : c.status === 'CANCELADA' ? `<div class="meta"><span>${t('det.cancelada', { u: esc(c.canceladoPor), d: dataHora(c.canceladoEm), m: esc(c.motivoCancelamento) })}</span></div>`
    : c.prazoEncerradoEm ? `<div class="aviso">${t('det.prazoEncerrado')}</div>` : '';
  const semPainel = andamento && !painelAbrir && !painelProposta && !painelDecisao ? `<p class="sub">${t('det.semPermissao')}</p>` : '';

  el.innerHTML = `
    <div class="cab"><div>
        <h3><span class="mono">${esc(c.codigo)}</span> · ${t('origem.' + c.origem)} ${tag(c.status)}</h3>
        <div class="meta"><span>${t('campo.dataLimite')}: <b>${data(c.dataLimite)}</b></span>
          <span>${t('campo.convidados')}: <b>${c.abertaAoMercado ? t('det.aberta') : c.fornecedoresConvidados.map(f => esc(f.razaoSocial)).join(', ')}</b></span>
          <span>${t('campo.usuario')}: <b>${esc(c.criadoPor)}</b></span></div>
        ${c.observacao ? `<div class="meta"><span>${t('campo.observacao')}: ${esc(c.observacao)}</span></div>` : ''}${situacao}
      </div>
      <div class="acoes"><button class="secundario" id="historico-cotacao">${t('acao.historico')}</button>
        ${andamento && comprador ? `<button class="secundario" id="cancelar-cotacao">${t('acao.cancelar')}</button>` : ''}
        <button class="secundario" id="fechar-detalhe">${t('acao.fechar')}</button></div>
    </div>
    ${itens}${propostas}
    ${andamento && c.propostas.length ? `<h4 class="titulo-recs">${t('det.recomendacoes')}</h4>
      <div class="recs">${blocoRecomendacao(c.recomendacaoMenorPreco, c.propostaVencedoraId)}${blocoRecomendacao(c.recomendacaoPontuacaoPonderada, c.propostaVencedoraId)}</div>` : ''}
    <div class="paineis">${painelAbrir}${painelProposta}${painelDecisao}</div>${semPainel}`;

  $('#fechar-detalhe').onclick = () => { el.hidden = true; estado.cotacaoAberta = null; };
  $('#historico-cotacao').onclick = async () => { try { mostrarHistorico(t('modal.historico', { c: c.codigo }), await api('GET', `/api/cotacoes/${id}/historico`)); } catch (e) { falhou(e); } };
  const btnCancelar = $('#cancelar-cotacao');
  if (btnCancelar) btnCancelar.onclick = () => pedirMotivo(t('modal.cancelarCotacao', { c: c.codigo }), async motivo => {
    await api('PATCH', `/api/cotacoes/${id}/cancelar`, { motivo });
    avisar(t('ok.cancelado'));
    recarregar();
  });
  $('#form-abrir')?.addEventListener('submit', async ev => {
    ev.preventDefault();
    const ids = [...ev.target.querySelectorAll('input[name=forn]:checked')].map(i => Number(i.value));
    try {
      await api('PATCH', `/api/cotacoes/${id}/abrir`, { fornecedoresIds: ids, dataLimite: ev.target.dataLimite.value || null });
      avisar(t('ok.aberta'));
      recarregar();
    } catch (e) { falhou(e); }
  });
  $('#form-proposta')?.addEventListener('submit', async ev => {
    ev.preventDefault();
    const f = ev.target;
    const corpo = {
      fornecedorId: Number(f.fornecedorId.value),
      itens: [...f.querySelectorAll('[data-produto]')].map(i => ({ produtoId: Number(i.dataset.produto), precoUnitario: i.value })),
      valorFrete: f.valorFrete.value || 0, impostoPercentual: f.impostoPercentual.value || 0, valorImpostoFixo: f.valorImpostoFixo.value || 0,
      prazoEntregaDias: f.prazoEntregaDias.value, dataValidade: f.dataValidade.value
    };
    try {
      const p = await api('POST', `/api/cotacoes/${id}/propostas`, corpo);
      avisar(t('ok.proposta', { total: moeda(p.valorTotal) }));
      recarregar();
    } catch (e) { falhou(e); }
  });
  $('#form-decidir')?.addEventListener('submit', async ev => {
    ev.preventDefault();
    try {
      const r = await api('POST', `/api/cotacoes/${id}/decidir`, dadosDoForm(ev.target));
      avisar(t('ok.decisao', { pedido: r.pedidoCompra.numeroPedido, fornecedor: r.propostaVencedora.fornecedorRazaoSocial }));
      recarregar();
    } catch (e) { falhou(e); }
  });
}

/* ---------------- pedidos ---------------- */
async function carregarPedidos() {
  const params = new URLSearchParams({ pagina: estado.pagina.pedidos, tamanho: 15 });
  if ($('#filtro-pedido').value) params.set('status', $('#filtro-pedido').value);
  const pagina = await api('GET', '/api/pedidos-compra?' + params);
  const aprovador = pode('APROVADOR');
  $('#tb-pedidos').innerHTML = pagina.conteudo.length ? pagina.conteudo.map(p => {
    const aberto = p.status === 'EMITIDO' || p.status === 'PARCIALMENTE_RECEBIDO';
    return `<tr><td class="mono">${esc(p.numeroPedido)}</td><td class="mono">${esc(p.cotacaoCodigo)}</td><td>${esc(p.fornecedorRazaoSocial)}</td>
      <td class="itens-celula">${p.itens.map(i => `<b>${esc(i.produtoSku)}</b> ${num(i.quantidadeRecebida)}/${num(i.quantidade)}`).join(' · ')}</td>
      <td class="num">${moeda(p.valorTotal)}</td><td>${tag(p.status)}</td>
      <td class="acoes"><button class="secundario" data-ver="${p.id}">${t('acao.ver')}</button>
        <button class="secundario" data-historico="${p.id}" data-numero="${esc(p.numeroPedido)}">${t('acao.historico')}</button>
        ${aberto && aprovador ? `<button class="primario" data-receber="${p.id}">${t('acao.receber')}</button>
        <button class="secundario" data-cancelar="${p.id}" data-numero="${esc(p.numeroPedido)}">${t('acao.cancelar')}</button>` : ''}</td></tr>`;
  }).join('') : `<tr><td colspan="7" class="vazio">${t('vazio')}</td></tr>`;
  paginador('pg-pedidos', pagina, p => { estado.pagina.pedidos = p; carregarPedidos().catch(falhou); });
}

function tabelaItensPedido(p, comEntrada) {
  return `<table class="subtabela"><thead><tr><th>${t('campo.produto')}</th><th class="num">${t('campo.quantidade')}</th>
      <th class="num">${t('campo.recebido')}</th><th class="num">${t('campo.pendente')}</th><th class="num">${t('campo.custoEfetivo')}</th>
      ${comEntrada ? `<th class="num">${t('acao.receber')}</th>` : ''}</tr></thead><tbody>${p.itens.map(i => `<tr>
      <td><span class="mono">${esc(i.produtoSku)}</span> · ${esc(i.produtoNome)}</td><td class="num">${num(i.quantidade)}</td>
      <td class="num">${num(i.quantidadeRecebida)}</td><td class="num">${num(i.quantidadePendente)}</td><td class="num">${moeda(i.custoUnitarioEfetivo)}</td>
      ${comEntrada ? `<td class="num"><input type="number" step="0.0001" min="0" max="${i.quantidadePendente}" value="${i.quantidadePendente}" data-receber-produto="${i.produtoId}"></td>` : ''}
      </tr>`).join('')}</tbody></table>`;
}

$('#tb-pedidos').addEventListener('click', async ev => {
  const b = ev.target.closest('button');
  if (!b) return;
  try {
    if (b.dataset.ver) {
      const p = await api('GET', `/api/pedidos-compra/${b.dataset.ver}`);
      abrirModal(`<h3>${t('modal.pedido', { c: esc(p.numeroPedido) })} ${tag(p.status)}</h3>
          <div class="meta"><span>${esc(p.fornecedorRazaoSocial)}</span><span>${t('campo.total')}: <b>${moeda(p.valorTotal)}</b></span>
          <span>${t('campo.usuario')}: <b>${esc(p.emitidoPor)}</b></span>${p.motivoCancelamento ? `<span>${t('campo.motivo')}: ${esc(p.motivoCancelamento)}</span>` : ''}</div>
          ${tabelaItensPedido(p, false)}<div class="modal-acoes"><button class="secundario" data-fechar>${t('acao.fechar')}</button></div>`);
    } else if (b.dataset.historico) {
      mostrarHistorico(t('modal.historico', { c: b.dataset.numero }), await api('GET', `/api/pedidos-compra/${b.dataset.historico}/historico`));
    } else if (b.dataset.receber) {
      const p = await api('GET', `/api/pedidos-compra/${b.dataset.receber}`);
      abrirModal(`<h3>${t('modal.receber', { c: esc(p.numeroPedido) })}</h3><p class="sub">${t('modal.receberDica')}</p><form>
          ${tabelaItensPedido(p, true)}
          <div class="modal-acoes"><button type="button" class="secundario" data-fechar>${t('acao.fechar')}</button><button class="primario">${t('acao.receber')}</button></div></form>`,
        async form => {
          const itens = [...form.querySelectorAll('[data-receber-produto]')]
            .map(i => ({ produtoId: Number(i.dataset.receberProduto), quantidade: Number(i.value) })).filter(i => i.quantidade > 0);
          await api('POST', `/api/pedidos-compra/${p.id}/receber`, { itens });
          avisar(t('ok.recebido', { pedido: p.numeroPedido }));
          recarregar();
        });
    } else if (b.dataset.cancelar) {
      pedirMotivo(t('modal.cancelarPedido', { c: b.dataset.numero }), async motivo => {
        await api('PATCH', `/api/pedidos-compra/${b.dataset.cancelar}/cancelar`, { motivo });
        avisar(t('ok.cancelado'));
        recarregar();
      });
    }
  } catch (e) { falhou(e); }
});
$('#filtro-pedido').addEventListener('change', () => { estado.pagina.pedidos = 0; carregarPedidos().catch(falhou); });

/* ---------------- usuários (ADMIN) ---------------- */
async function carregarUsuarios() {
  if (!pode('ADMIN')) return;
  const lista = await api('GET', '/api/usuarios');
  $('#tb-usuarios').innerHTML = lista.map(u => `<tr><td class="mono">${esc(u.username)}</td><td>${esc(u.nome)}</td><td>${esc(u.perfil)}</td>
      <td>${u.ativo ? tag('ok', t('status.ativo')) : tag('off', t('status.inativo'))}</td>
      <td class="acoes">${u.username === estado.usuario.username ? '' : `<button class="secundario" data-usuario="${u.id}" data-ativo="${!u.ativo}">${t(u.ativo ? 'acao.inativar' : 'acao.ativar')}</button>`}</td></tr>`).join('');
}
$('#tb-usuarios').addEventListener('click', async ev => {
  const b = ev.target.closest('[data-usuario]');
  if (!b) return;
  try { await api('PATCH', `/api/usuarios/${b.dataset.usuario}/status`, { ativo: b.dataset.ativo === 'true' }); avisar(t('ok.status')); carregarUsuarios(); } catch (e) { falhou(e); }
});
$('#form-usuario').addEventListener('submit', async ev => {
  ev.preventDefault();
  try { await api('POST', '/api/usuarios', dadosDoForm(ev.target)); avisar(t('ok.usuario')); ev.target.reset(); carregarUsuarios(); } catch (e) { falhou(e); }
});

/* ---------------- inicialização ---------------- */
document.querySelectorAll('[data-aba]').forEach(b => b.addEventListener('click', () => mostrarAba(b.dataset.aba)));
document.querySelectorAll('[data-lang]').forEach(b => b.addEventListener('click', () => {
  estado.lang = b.dataset.lang;
  localStorage.setItem('lang', estado.lang);
  aplicarIdioma();
  recarregar();
}));
aplicarIdioma();
(async () => {
  if (!estado.token) return mostrarLogin();
  try {
    estado.usuario = await api('GET', '/api/auth/eu');
    entrarNoApp();
  } catch (e) { mostrarLogin(); }
})();
