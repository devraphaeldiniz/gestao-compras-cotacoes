'use strict';

const I18N = {
  pt: {
    'aba.produtos': 'Produtos', 'aba.fornecedores': 'Fornecedores', 'aba.cotacoes': 'Cotações', 'aba.pedidos': 'Pedidos de compra',
    'kpi.necessidade': 'Produtos no estoque mínimo', 'kpi.rascunhos': 'Cotações em rascunho', 'kpi.abertas': 'Cotações abertas', 'kpi.emitidos': 'Pedidos a receber',
    'produtos.titulo': 'Produtos e estoque', 'produtos.sub': 'Quando o saldo chega ao estoque mínimo, uma cotação de reposição é criada automaticamente.',
    'produtos.novo': 'Novo produto', 'produtos.movimentar': 'Movimentar estoque', 'produtos.filtro': 'Só produtos com necessidade de compra',
    'fornecedores.titulo': 'Fornecedores', 'fornecedores.sub': 'Propostas de fornecedores inativos deixam de valer na decisão.', 'fornecedores.novo': 'Novo fornecedor',
    'cotacoes.titulo': 'Cotações', 'cotacoes.sub': 'Clique numa cotação para abri-la, registrar propostas e decidir a vencedora.',
    'pedidos.titulo': 'Pedidos de compra', 'pedidos.sub': 'Gerados na decisão. O recebimento dá entrada no estoque e recalcula o custo médio.',
    'campo.sku': 'SKU', 'campo.nome': 'Nome', 'campo.unidade': 'Unid.', 'campo.saldo': 'Saldo', 'campo.minimo': 'Estoque mínimo',
    'campo.ultimoPreco': 'Último custo', 'campo.precoMedio': 'Custo médio', 'campo.produto': 'Produto', 'campo.tipo': 'Tipo',
    'campo.quantidade': 'Quantidade', 'campo.razaoSocial': 'Razão social', 'campo.documento': 'CPF/CNPJ', 'campo.email': 'E-mail',
    'campo.prazoMedio': 'Prazo médio (dias)', 'campo.status': 'Status', 'campo.codigo': 'Código', 'campo.propostas': 'Propostas',
    'campo.dataLimite': 'Data limite', 'campo.numero': 'Número', 'campo.cotacao': 'Cotação', 'campo.fornecedor': 'Fornecedor',
    'campo.total': 'Valor total', 'campo.prazo': 'Prazo (dias)', 'campo.preco': 'Preço unit.', 'campo.frete': 'Frete',
    'campo.impostoPct': 'Imposto (%)', 'campo.impostoFixo': 'Imposto fixo', 'campo.validade': 'Validade', 'campo.imposto': 'Imposto',
    'campo.mercadoria': 'Mercadoria', 'campo.pontuacao': 'Pontuação', 'campo.criterio': 'Critério', 'campo.escolha': 'Proposta escolhida',
    'campo.justificativa': 'Justificativa', 'campo.convidados': 'Fornecedores convidados', 'campo.observacao': 'Observação',
    'acao.cadastrar': 'Cadastrar', 'acao.movimentar': 'Registrar movimentação', 'acao.ativar': 'Ativar', 'acao.inativar': 'Inativar',
    'acao.abrir': 'Abrir cotação', 'acao.registrar': 'Registrar proposta', 'acao.decidir': 'Decidir e emitir pedido',
    'acao.receber': 'Receber', 'acao.fechar': 'Fechar',
    'mov.SAIDA': 'Saída', 'mov.ENTRADA': 'Entrada', 'mov.AJUSTE': 'Ajuste (novo saldo)',
    'filtro.todos': 'Todos', 'filtro.ativos': 'Ativos', 'filtro.inativos': 'Inativos', 'filtro.todas': 'Todas',
    'status.RASCUNHO': 'Rascunho', 'status.ABERTA': 'Aberta', 'status.FECHADA': 'Fechada', 'status.CANCELADA': 'Cancelada',
    'status.PENDENTE': 'Pendente', 'status.VENCEDORA': 'Vencedora', 'status.REJEITADA': 'Rejeitada', 'status.DESQUALIFICADA': 'Desqualificada',
    'status.EMITIDO': 'Emitido', 'status.RECEBIDO': 'Recebido', 'status.CANCELADO': 'Cancelado',
    'status.ativo': 'Ativo', 'status.inativo': 'Inativo', 'status.comprar': 'Comprar',
    'crit.MENOR_PRECO': 'Menor preço', 'crit.PONTUACAO_PONDERADA': 'Pontuação ponderada (70% preço / 30% prazo)',
    'det.recomendacoes': 'Recomendação por critério', 'det.semRanking': 'Ainda não há propostas elegíveis.',
    'det.minimo': 'São necessárias {min} propostas válidas para decidir. Elegíveis agora: {n}.',
    'det.abrir': 'Abrir para propostas', 'det.mercado': 'Deixe todos desmarcados para abrir ao mercado.',
    'det.proposta': 'Nova proposta', 'det.decisao': 'Decisão', 'det.automatica': 'A recomendada pelo critério',
    'det.justificativaDica': 'Obrigatória se a escolha for diferente da recomendada.', 'det.aberta': 'Aberta ao mercado',
    'det.dias': 'dias', 'det.pts': 'pts', 'det.vencedora': 'vencedora',
    'ok.produto': 'Produto cadastrado.', 'ok.produtoCotacao': 'Produto cadastrado. Estoque no mínimo: cotação de reposição criada.',
    'ok.movimento': 'Estoque atualizado.', 'ok.movimentoCotacao': 'Estoque atualizado. Produto no mínimo: confira a aba Cotações.',
    'ok.fornecedor': 'Fornecedor cadastrado.', 'ok.status': 'Status do fornecedor atualizado.', 'ok.aberta': 'Cotação aberta para propostas.',
    'ok.proposta': 'Proposta registrada: total {total}.', 'ok.decisao': 'Pedido {pedido} emitido para {fornecedor}',
    'ok.recebido': 'Pedido {pedido} recebido. Estoque e custo médio atualizados.',
    'vazio': 'Nada por aqui ainda.', 'erro.rede': 'Não foi possível falar com a API.'
  },
  en: {
    'aba.produtos': 'Products', 'aba.fornecedores': 'Suppliers', 'aba.cotacoes': 'Quotations', 'aba.pedidos': 'Purchase orders',
    'kpi.necessidade': 'Products at minimum stock', 'kpi.rascunhos': 'Draft quotations', 'kpi.abertas': 'Open quotations', 'kpi.emitidos': 'Orders to receive',
    'produtos.titulo': 'Products and stock', 'produtos.sub': 'When the balance reaches the minimum stock, a replenishment quotation is created automatically.',
    'produtos.novo': 'New product', 'produtos.movimentar': 'Stock movement', 'produtos.filtro': 'Only products that need purchasing',
    'fornecedores.titulo': 'Suppliers', 'fornecedores.sub': 'Proposals from inactive suppliers no longer count in the decision.', 'fornecedores.novo': 'New supplier',
    'cotacoes.titulo': 'Quotations', 'cotacoes.sub': 'Click a quotation to open it, register proposals and pick the winner.',
    'pedidos.titulo': 'Purchase orders', 'pedidos.sub': 'Created by the decision. Receiving adds to stock and recalculates the average cost.',
    'campo.sku': 'SKU', 'campo.nome': 'Name', 'campo.unidade': 'Unit', 'campo.saldo': 'Balance', 'campo.minimo': 'Minimum stock',
    'campo.ultimoPreco': 'Last cost', 'campo.precoMedio': 'Average cost', 'campo.produto': 'Product', 'campo.tipo': 'Type',
    'campo.quantidade': 'Quantity', 'campo.razaoSocial': 'Company name', 'campo.documento': 'Tax ID (CPF/CNPJ)', 'campo.email': 'E-mail',
    'campo.prazoMedio': 'Average lead time (days)', 'campo.status': 'Status', 'campo.codigo': 'Code', 'campo.propostas': 'Proposals',
    'campo.dataLimite': 'Deadline', 'campo.numero': 'Number', 'campo.cotacao': 'Quotation', 'campo.fornecedor': 'Supplier',
    'campo.total': 'Total', 'campo.prazo': 'Lead time (days)', 'campo.preco': 'Unit price', 'campo.frete': 'Freight',
    'campo.impostoPct': 'Tax (%)', 'campo.impostoFixo': 'Fixed tax', 'campo.validade': 'Valid until', 'campo.imposto': 'Tax',
    'campo.mercadoria': 'Goods', 'campo.pontuacao': 'Score', 'campo.criterio': 'Criterion', 'campo.escolha': 'Chosen proposal',
    'campo.justificativa': 'Justification', 'campo.convidados': 'Invited suppliers', 'campo.observacao': 'Notes',
    'acao.cadastrar': 'Create', 'acao.movimentar': 'Record movement', 'acao.ativar': 'Activate', 'acao.inativar': 'Deactivate',
    'acao.abrir': 'Open quotation', 'acao.registrar': 'Register proposal', 'acao.decidir': 'Decide and issue order',
    'acao.receber': 'Receive', 'acao.fechar': 'Close',
    'mov.SAIDA': 'Withdrawal', 'mov.ENTRADA': 'Receipt', 'mov.AJUSTE': 'Adjustment (new balance)',
    'filtro.todos': 'All', 'filtro.ativos': 'Active', 'filtro.inativos': 'Inactive', 'filtro.todas': 'All',
    'status.RASCUNHO': 'Draft', 'status.ABERTA': 'Open', 'status.FECHADA': 'Closed', 'status.CANCELADA': 'Cancelled',
    'status.PENDENTE': 'Pending', 'status.VENCEDORA': 'Winner', 'status.REJEITADA': 'Rejected', 'status.DESQUALIFICADA': 'Disqualified',
    'status.EMITIDO': 'Issued', 'status.RECEBIDO': 'Received', 'status.CANCELADO': 'Cancelled',
    'status.ativo': 'Active', 'status.inativo': 'Inactive', 'status.comprar': 'Reorder',
    'crit.MENOR_PRECO': 'Lowest price', 'crit.PONTUACAO_PONDERADA': 'Weighted score (70% price / 30% lead time)',
    'det.recomendacoes': 'Recommendation by criterion', 'det.semRanking': 'No eligible proposals yet.',
    'det.minimo': '{min} valid proposals are required to decide. Eligible now: {n}.',
    'det.abrir': 'Open for proposals', 'det.mercado': 'Leave all unchecked to open it to the market.',
    'det.proposta': 'New proposal', 'det.decisao': 'Decision', 'det.automatica': 'The one recommended by the criterion',
    'det.justificativaDica': 'Required when the choice differs from the recommendation.', 'det.aberta': 'Open to the market',
    'det.dias': 'days', 'det.pts': 'pts', 'det.vencedora': 'winner',
    'ok.produto': 'Product created.', 'ok.produtoCotacao': 'Product created. Stock at minimum: replenishment quotation created.',
    'ok.movimento': 'Stock updated.', 'ok.movimentoCotacao': 'Stock updated. Product at minimum: check the Quotations tab.',
    'ok.fornecedor': 'Supplier created.', 'ok.status': 'Supplier status updated.', 'ok.aberta': 'Quotation opened for proposals.',
    'ok.proposta': 'Proposal registered: total {total}.', 'ok.decisao': 'Order {pedido} issued to {fornecedor}',
    'ok.recebido': 'Order {pedido} received. Stock and average cost updated.',
    'vazio': 'Nothing here yet.', 'erro.rede': 'Could not reach the API.'
  }
};

const idiomaInicial = () => {
  const salvo = localStorage.getItem('lang');
  if (salvo === 'pt' || salvo === 'en') return salvo;
  return (navigator.language || 'pt').toLowerCase().startsWith('en') ? 'en' : 'pt';
};
const estado = { lang: idiomaInicial(), aba: location.hash.replace('#', '') || 'produtos', cotacaoAberta: null };

const $ = (s, el = document) => el.querySelector(s);
const t = (k, vars = {}) => (I18N[estado.lang][k] ?? k).replace(/\{(\w+)\}/g, (_, v) => vars[v] ?? '');
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const locale = () => estado.lang === 'en' ? 'en-US' : 'pt-BR';
const moeda = v => v == null ? '–' : new Intl.NumberFormat(locale(), { style: 'currency', currency: 'BRL' }).format(v);
const num = (v, max = 4) => v == null ? '–' : new Intl.NumberFormat(locale(), { maximumFractionDigits: max }).format(v);
const data = v => v ? new Intl.DateTimeFormat(locale()).format(new Date(v.length === 10 ? v + 'T00:00:00' : v)) : '–';
const tag = (status, texto) => `<span class="tag t-${esc(status)}">${esc(texto ?? t('status.' + status))}</span>`;
const hojeISO = (dias = 0) => { const d = new Date(); d.setDate(d.getDate() + dias); return d.toLocaleDateString('sv-SE'); };

/** Toda chamada envia o idioma escolhido: as mensagens de erro já voltam traduzidas pelo backend. */
async function api(metodo, caminho, corpo) {
  let resposta;
  try {
    resposta = await fetch(caminho, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', 'Accept-Language': estado.lang === 'en' ? 'en' : 'pt-BR' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo)
    });
  } catch (e) { throw { mensagem: t('erro.rede') }; }
  const texto = await resposta.text();
  const json = texto ? JSON.parse(texto) : null;
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

/* ---------------- i18n e navegação ---------------- */
function aplicarIdioma() {
  document.documentElement.lang = estado.lang === 'en' ? 'en' : 'pt-BR';
  document.querySelectorAll('[data-i18n]').forEach(el => el.textContent = t(el.dataset.i18n));
  document.querySelectorAll('[data-lang]').forEach(b => b.classList.toggle('ativo', b.dataset.lang === estado.lang));
}
function mostrarAba(aba) {
  estado.aba = aba;
  history.replaceState(null, '', '#' + aba);
  document.querySelectorAll('[data-view]').forEach(s => s.hidden = s.dataset.view !== aba);
  document.querySelectorAll('[data-aba]').forEach(b => b.classList.toggle('ativo', b.dataset.aba === aba));
  recarregar();
}
const carregadores = () => ({ produtos: carregarProdutos, fornecedores: carregarFornecedores, cotacoes: carregarCotacoes, pedidos: carregarPedidos });
async function recarregar() {
  try {
    await Promise.all([carregarKpis(), carregadores()[estado.aba]()]);
    if (estado.aba === 'cotacoes' && estado.cotacaoAberta) await abrirDetalhe(estado.cotacaoAberta);
  } catch (e) { falhou(e); }
}

async function carregarKpis() {
  const [necessidade, cotacoes, pedidos] = await Promise.all([
    api('GET', '/api/produtos?apenasNecessidadeCompra=true'), api('GET', '/api/cotacoes'), api('GET', '/api/pedidos-compra?status=EMITIDO')]);
  $('#kpi-necessidade').textContent = necessidade.length;
  $('#kpi-rascunhos').textContent = cotacoes.filter(c => c.status === 'RASCUNHO').length;
  $('#kpi-abertas').textContent = cotacoes.filter(c => c.status === 'ABERTA').length;
  $('#kpi-emitidos').textContent = pedidos.length;
}

/* ---------------- Produtos ---------------- */
async function carregarProdutos() {
  const apenas = $('#filtro-necessidade').checked;
  const todos = await api('GET', '/api/produtos');
  const lista = apenas ? todos.filter(p => p.necessitaCompra) : todos;
  $('#tb-produtos').innerHTML = lista.length ? lista.map(p => `<tr>
      <td class="mono">${esc(p.sku)}</td><td>${esc(p.nome)}</td><td>${esc(p.unidade)}</td>
      <td class="num">${num(p.saldoAtual)}</td><td class="num">${num(p.estoqueMinimo)}</td>
      <td class="num">${moeda(p.ultimoPrecoCompra)}</td><td class="num">${moeda(p.precoMedioCompra)}</td>
      <td>${p.necessitaCompra ? tag('alerta', t('status.comprar')) : ''}</td></tr>`).join('')
    : `<tr><td colspan="8" class="vazio">${t('vazio')}</td></tr>`;
  const sel = $('#sel-produto-estoque'), atual = sel.value;
  sel.innerHTML = todos.map(p => `<option value="${p.id}">${esc(p.sku)} · ${esc(p.nome)}</option>`).join('');
  if (atual) sel.value = atual;
}

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
    recarregar();
  } catch (e) { falhou(e); }
});
$('#filtro-necessidade').addEventListener('change', () => carregarProdutos().catch(falhou));

/* ---------------- Fornecedores ---------------- */
async function carregarFornecedores() {
  const filtro = $('#filtro-fornecedor').value;
  const lista = await api('GET', '/api/fornecedores' + (filtro ? '?ativo=' + filtro : ''));
  $('#tb-fornecedores').innerHTML = lista.length ? lista.map(f => `<tr>
      <td>${esc(f.razaoSocial)}</td><td class="mono">${esc(f.documento)}</td><td>${esc(f.email)}</td>
      <td class="num">${f.prazoMedioDias}</td><td>${f.ativo ? tag('ok', t('status.ativo')) : tag('off', t('status.inativo'))}</td>
      <td class="acoes"><button class="secundario" data-status="${f.id}" data-ativo="${!f.ativo}">${t(f.ativo ? 'acao.inativar' : 'acao.ativar')}</button></td></tr>`).join('')
    : `<tr><td colspan="6" class="vazio">${t('vazio')}</td></tr>`;
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
$('#filtro-fornecedor').addEventListener('change', () => carregarFornecedores().catch(falhou));

/* ---------------- Cotações ---------------- */
async function carregarCotacoes() {
  const filtro = $('#filtro-cotacao').value;
  const lista = await api('GET', '/api/cotacoes' + (filtro ? '?status=' + filtro : ''));
  $('#tb-cotacoes').innerHTML = lista.length ? lista.map(c => `<tr class="clicavel" data-cotacao="${c.id}">
      <td class="mono">${esc(c.codigo)}</td><td>${esc(c.produtoSku)} · ${esc(c.produtoNome)}</td>
      <td class="num">${num(c.quantidade)} ${esc(c.unidade)}</td><td>${tag(c.status)}</td>
      <td class="num">${c.quantidadePropostas}</td><td>${data(c.dataLimite)}</td></tr>`).join('')
    : `<tr><td colspan="6" class="vazio">${t('vazio')}</td></tr>`;
}
$('#tb-cotacoes').addEventListener('click', ev => {
  const linha = ev.target.closest('[data-cotacao]');
  if (linha) abrirDetalhe(Number(linha.dataset.cotacao)).then(() => $('#detalhe').scrollIntoView({ behavior: 'smooth' })).catch(falhou);
});
$('#filtro-cotacao').addEventListener('change', () => carregarCotacoes().catch(falhou));

function blocoRecomendacao(rec, vencedoraId) {
  const itens = rec.ranking.map(i => `<li>${esc(i.fornecedorRazaoSocial)} — ${moeda(i.valorTotal)}
      <span>· ${i.prazoEntregaDias} ${t('det.dias')} · ${num(i.pontuacao, 2)} ${t('det.pts')}${i.propostaId === vencedoraId ? ' · ' + t('det.vencedora') : ''}</span></li>`).join('');
  return `<div class="rec"><h4>${t('crit.' + rec.criterio)}</h4>${itens ? `<ol>${itens}</ol>` : `<span class="sub">${t('det.semRanking')}</span>`}</div>`;
}

async function abrirDetalhe(id) {
  estado.cotacaoAberta = id;
  const [c, fornecedores] = await Promise.all([api('GET', `/api/cotacoes/${id}`), api('GET', '/api/fornecedores?ativo=true')]);
  const el = $('#detalhe');
  el.hidden = false;

  const propostas = c.propostas.length ? `<table><thead><tr>
      <th>${t('campo.fornecedor')}</th><th class="num">${t('campo.preco')}</th><th class="num">${t('campo.mercadoria')}</th>
      <th class="num">${t('campo.frete')}</th><th class="num">${t('campo.imposto')}</th><th class="num">${t('campo.total')}</th>
      <th class="num">${t('campo.prazo')}</th><th>${t('campo.validade')}</th><th class="num">${t('campo.pontuacao')}</th><th>${t('campo.status')}</th>
    </tr></thead><tbody>${c.propostas.map(p => `<tr>
      <td>${esc(p.fornecedorRazaoSocial)}${p.fornecedorAtivo ? '' : ' ' + tag('off', t('status.inativo'))}</td>
      <td class="num">${moeda(p.precoUnitario)}</td><td class="num">${moeda(p.valorMercadoria)}</td><td class="num">${moeda(p.valorFrete)}</td>
      <td class="num">${moeda(p.valorImpostoCalculado)}</td><td class="num"><b>${moeda(p.valorTotal)}</b></td>
      <td class="num">${p.prazoEntregaDias}</td><td>${data(p.dataValidade)}</td><td class="num">${num(p.pontuacaoCalculada, 2)}</td>
      <td>${tag(p.status)}</td></tr>`).join('')}</tbody></table>` : `<p class="vazio">${t('vazio')}</p>`;

  const convidadosIds = new Set(c.fornecedoresConvidados.map(f => f.id));
  const aberta = c.status === 'ABERTA';
  const podemEnviar = c.abertaAoMercado ? fornecedores : fornecedores.filter(f => convidadosIds.has(f.id));

  const painelAbrir = c.status === 'RASCUNHO' ? `<div class="card"><h3>${t('det.abrir')}</h3><form id="form-abrir">
      <label>${t('campo.convidados')}<div class="checks">${fornecedores.map(f =>
        `<label><input type="checkbox" name="forn" value="${f.id}"> ${esc(f.razaoSocial)}</label>`).join('')}</div>
        <span>${t('det.mercado')}</span></label>
      <label>${t('campo.dataLimite')}<input type="date" name="dataLimite" min="${hojeISO()}" value="${hojeISO(7)}"></label>
      <button class="primario">${t('acao.abrir')}</button></form></div>` : '';

  const painelProposta = aberta ? `<div class="card"><h3>${t('det.proposta')}</h3><form id="form-proposta">
      <label>${t('campo.fornecedor')}<select name="fornecedorId" required>${podemEnviar.map(f =>
        `<option value="${f.id}">${esc(f.razaoSocial)}</option>`).join('')}</select></label>
      <div class="linha"><label>${t('campo.preco')}<input name="precoUnitario" type="number" step="0.0001" min="0.0001" required></label>
        <label>${t('campo.frete')}<input name="valorFrete" type="number" step="0.01" min="0" value="0"></label></div>
      <div class="linha"><label>${t('campo.impostoPct')}<input name="impostoPercentual" type="number" step="0.0001" min="0" max="100" value="0"></label>
        <label>${t('campo.impostoFixo')}<input name="valorImpostoFixo" type="number" step="0.01" min="0" value="0"></label></div>
      <div class="linha"><label>${t('campo.prazo')}<input name="prazoEntregaDias" type="number" min="0" required></label>
        <label>${t('campo.validade')}<input name="dataValidade" type="date" min="${hojeISO()}" value="${hojeISO(30)}" required></label></div>
      <button class="primario">${t('acao.registrar')}</button></form></div>` : '';

  const elegiveis = c.propostas.filter(p => p.status === 'PENDENTE' && p.fornecedorAtivo);
  const painelDecisao = aberta ? `<div class="card"><h3>${t('det.decisao')}</h3>
      ${c.podeDecidir ? '' : `<div class="aviso">${t('det.minimo', { min: c.minimoPropostas, n: c.propostasElegiveis })}</div>`}
      <form id="form-decidir">
      <label>${t('campo.criterio')}<select name="criterio">
        <option value="MENOR_PRECO">${t('crit.MENOR_PRECO')}</option>
        <option value="PONTUACAO_PONDERADA">${t('crit.PONTUACAO_PONDERADA')}</option></select></label>
      <label>${t('campo.escolha')}<select name="propostaIdEscolhida"><option value="">${t('det.automatica')}</option>
        ${elegiveis.map(p => `<option value="${p.id}">${esc(p.fornecedorRazaoSocial)} — ${moeda(p.valorTotal)}</option>`).join('')}</select></label>
      <label>${t('campo.justificativa')}<textarea name="justificativaEscolha" rows="2" maxlength="500" placeholder="${t('det.justificativaDica')}"></textarea></label>
      <button class="primario" ${c.podeDecidir ? '' : 'disabled'}>${t('acao.decidir')}</button></form></div>` : '';

  el.innerHTML = `
    <div class="cab">
      <div>
        <h3><span class="mono">${esc(c.codigo)}</span> · ${esc(c.produtoSku)} — ${esc(c.produtoNome)} ${tag(c.status)}</h3>
        <div class="meta">
          <span>${t('campo.quantidade')}: <b>${num(c.quantidade)} ${esc(c.unidade)}</b></span>
          <span>${t('campo.saldo')}: <b>${num(c.saldoAtualProduto)}</b> / ${t('campo.minimo')}: <b>${num(c.estoqueMinimoProduto)}</b></span>
          <span>${t('campo.dataLimite')}: <b>${data(c.dataLimite)}</b></span>
          <span>${t('campo.convidados')}: <b>${c.abertaAoMercado ? t('det.aberta') : c.fornecedoresConvidados.map(f => esc(f.razaoSocial)).join(', ')}</b></span>
        </div>
        ${c.observacao ? `<div class="meta"><span>${t('campo.observacao')}: ${esc(c.observacao)}</span></div>` : ''}
      </div>
      <button class="secundario" id="fechar-detalhe">${t('acao.fechar')}</button>
    </div>
    ${propostas}
    ${c.status === 'FECHADA' || c.status === 'CANCELADA' ? '' : `
    <h4 class="titulo-recs">${t('det.recomendacoes')}</h4>
    <div class="recs">${blocoRecomendacao(c.recomendacaoMenorPreco, c.propostaVencedoraId)}${blocoRecomendacao(c.recomendacaoPontuacaoPonderada, c.propostaVencedoraId)}</div>`}
    <div class="paineis">${painelAbrir}${painelProposta}${painelDecisao}</div>`;

  $('#fechar-detalhe').onclick = () => { el.hidden = true; estado.cotacaoAberta = null; };
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
    try {
      const p = await api('POST', `/api/cotacoes/${id}/propostas`, dadosDoForm(ev.target));
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

/* ---------------- Pedidos ---------------- */
async function carregarPedidos() {
  const lista = await api('GET', '/api/pedidos-compra');
  $('#tb-pedidos').innerHTML = lista.length ? lista.map(p => `<tr>
      <td class="mono">${esc(p.numeroPedido)}</td><td class="mono">${esc(p.cotacaoCodigo)}</td><td>${esc(p.fornecedorRazaoSocial)}</td>
      <td>${esc(p.produtoSku)}</td><td class="num">${num(p.quantidade)}</td><td class="num">${moeda(p.valorTotal)}</td>
      <td class="num">${p.prazoEntregaDias}</td><td>${tag(p.status)}${p.dataRecebimento ? ' <span class="sub">' + data(p.dataRecebimento) + '</span>' : ''}</td>
      <td class="acoes">${p.status === 'EMITIDO' ? `<button class="primario" data-receber="${p.id}" data-numero="${esc(p.numeroPedido)}">${t('acao.receber')}</button>` : ''}</td></tr>`).join('')
    : `<tr><td colspan="9" class="vazio">${t('vazio')}</td></tr>`;
}
$('#tb-pedidos').addEventListener('click', async ev => {
  const b = ev.target.closest('[data-receber]');
  if (!b) return;
  try {
    await api('POST', `/api/pedidos-compra/${b.dataset.receber}/receber`);
    avisar(t('ok.recebido', { pedido: b.dataset.numero }));
    recarregar();
  } catch (e) { falhou(e); }
});

/* ---------------- Inicialização ---------------- */
document.querySelectorAll('[data-aba]').forEach(b => b.addEventListener('click', () => mostrarAba(b.dataset.aba)));
document.querySelectorAll('[data-lang]').forEach(b => b.addEventListener('click', () => {
  estado.lang = b.dataset.lang;
  localStorage.setItem('lang', estado.lang);
  aplicarIdioma();
  recarregar();
}));
aplicarIdioma();
mostrarAba(Object.keys(carregadores()).includes(estado.aba) ? estado.aba : 'produtos');
