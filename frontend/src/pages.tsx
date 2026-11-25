import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from './api';
import { Content, Field, Form, Grid, Panel, date, money, useData } from './ui';

type Product = {id: number; sku: string; name: string; price: number; stock: number};
type Customer = {id: number; name: string; email: string};
type Order = {id: number; customerId: number; status: 'DRAFT' | 'CONFIRMED' | 'CANCELED'; createdAt: string; total: number};
type Item = {productId: number; quantity: number; unitPrice: number};
type Movement = {id: number; productId: number; orderId: number | null; kind: 'ENTRY' | 'SALE' | 'RETURN'; quantity: number; createdAt: string};
type Indicators = Record<'products' | 'customers' | 'lowStock' | 'drafts' | 'confirmed' | 'canceled' | 'confirmedTotal', number>;

const products = () => api<Product[]>('/products');
const customers = () => api<Customer[]>('/customers');
const status = {DRAFT: 'Rascunho', CONFIRMED: 'Confirmado', CANCELED: 'Cancelado'};
const kind = {ENTRY: 'Entrada', SALE: 'Saída por venda', RETURN: 'Estorno'};

export function Dashboard() {
  const data = useData(() => api<Indicators>('/dashboard'));
  return <><h1>Visão geral</h1><Content data={data}>{d => <>
    <div className="summary"><span>Vendas em pedidos confirmados</span><strong>{money(d.confirmedTotal)}</strong></div>
    <div className="metrics">{([
      ['Produtos', d.products], ['Clientes', d.customers], ['Produtos com até 5 unidades', d.lowStock],
      ['Pedidos em rascunho', d.drafts], ['Pedidos confirmados', d.confirmed], ['Pedidos cancelados', d.canceled],
    ] as const).map(([label, value]) => <div className="panel metric" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
  </>}</Content></>;
}

export function Products() {
  const data = useData(products);
  return <><h1>Produtos</h1><Panel title="Cadastrar produto">
    <Form label="Salvar produto" submit={async form => {
      await api('/products', { sku:form.get('sku'), name:form.get('name'), price:form.get('price') }); data.refresh();
    }}>
      <Field label="SKU"><input className="form-control" name="sku" required maxLength={40} /></Field>
      <Field label="Nome"><input className="form-control" name="name" required maxLength={120} /></Field>
      <Field label="Preço unitário (R$)"><input className="form-control" name="price" type="number" min="0" max="9999999999.99" step=".01" required /></Field>
    </Form></Panel><Panel title="Produtos cadastrados"><Content data={data}>{list =>
      <Grid headers={['SKU','Nome','Preço','Saldo']} rows={list.map(p => [p.sku,p.name,money(p.price),p.stock])} />}
    </Content></Panel></>;
}

export function Customers() {
  const data = useData(customers);
  return <><h1>Clientes</h1><Panel title="Cadastrar cliente">
    <Form label="Salvar cliente" submit={async form => {
      await api('/customers', Object.fromEntries(form)); data.refresh();
    }}>
      <Field label="Nome"><input className="form-control" name="name" required maxLength={120} /></Field>
      <Field label="E-mail"><input className="form-control" name="email" type="email" required maxLength={180} /></Field>
    </Form></Panel><Panel title="Clientes cadastrados"><Content data={data}>{list =>
      <Grid headers={['Nome','E-mail']} rows={list.map(c => [c.name,c.email])} />}
    </Content></Panel></>;
}

function ProductSelect({list, name = 'productId'}: {list: Product[]; name?: string}) {
  return <select className="form-select" name={name} required defaultValue="">
    <option value="" disabled>Selecione</option>
    {list.map(p => <option value={p.id} key={p.id}>{p.sku} · {p.name} ({p.stock} em estoque)</option>)}
  </select>;
}

export function Inventory({admin}: {admin: boolean}) {
  const [filter, setFilter] = useState('');
  const catalog = useData(products);
  const history = useData(() => api<Movement[]>('/inventory/movements' + (filter ? '?productId=' + filter : '')), filter);
  return <><h1>Estoque</h1><Content data={catalog}>{list => <>
    {admin && <Panel title="Registrar entrada">{list.length === 0 ? <p>Cadastre um produto para registrar entradas.</p> :
      <Form label="Registrar entrada" submit={async form => {
        await api('/inventory/entries', {productId: Number(form.get('productId')), quantity:Number(form.get('quantity'))});
        catalog.refresh(); history.refresh();
      }}>
        <Field label="Produto"><ProductSelect list={list} /></Field>
        <Field label="Quantidade"><input name="quantity" type="number" min="1" max="2147483647" required className="form-control" /></Field>
      </Form>}</Panel>}
    <Panel title="Saldo por produto">
      <Grid headers={['SKU','Produto','Unidades']} rows={list.map(p => [p.sku,p.name,p.stock])} />
    </Panel>
    <Panel title="Últimas 100 movimentações">
      <Field label="Filtrar produto"><select className="form-select mb-3" value={filter} onChange={e => setFilter(e.target.value)}>
        <option value="">Todos os produtos</option>{list.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select></Field>
      <Content data={history}>{listMoves => <Grid headers={['Data','Produto','Movimento','Unidades','Pedido']}
        rows={listMoves.map(m => [date(m.createdAt),list.find(p => p.id === m.productId)?.name ?? m.productId,kind[m.kind],m.quantity,m.orderId ? <Link to={'/pedidos/' + m.orderId}>#{m.orderId}</Link> : '—'])} />}
      </Content>
    </Panel>
  </>}</Content></>;
}

export function Orders() {
  const data = useData(async () => {
    const [orders, clients, catalog] = await Promise.all([api<Order[]>('/orders'), customers(), products()]);
    return {orders, clients, catalog};
  });
  const [rows, setRows] = useState([0]);
  const [next, setNext] = useState(1);
  return <><h1>Pedidos</h1><Content data={data}>{({orders, clients, catalog}) => <>
    <Panel title="Criar pedido">{!clients.length || !catalog.length ? <p>Cadastre um cliente e um produto para criar pedidos.</p> :
      <Form label="Salvar rascunho" submit={async form => {
        await api('/orders', {customerId:Number(form.get('customerId')),items:rows.map(id => ({
          productId:Number(form.get('product-' + id)), quantity:Number(form.get('quantity-' + id)),
        }))});
        setRows([0]); data.refresh();
      }}>
        <Field label="Cliente"><select name="customerId" className="form-select" defaultValue="" required>
          <option value="" disabled>Selecione</option>{clients.map(c => <option key={c.id} value={c.id}>{c.name} · {c.email}</option>)}
        </select></Field>
        {rows.map((id, index) => <div key={id} className="order-row">
          <Field label={'Produto ' + (index + 1)}><ProductSelect list={catalog} name={'product-' + id} /></Field>
          <Field label="Quantidade"><input type="number" name={'quantity-' + id} min="1" max="1000000" required className="form-control" /></Field>
          {rows.length > 1 && <button type="button" className="btn btn-outline-secondary" aria-label={'Remover item ' + (index + 1)} onClick={() => setRows(rows.filter(row => row !== id))}>Remover</button>}
        </div>)}
        <div><button type="button" className="btn btn-outline-secondary" disabled={rows.length >= Math.min(100, catalog.length)} onClick={() => {setRows([...rows, next]); setNext(next + 1);}}>Adicionar item</button></div>
      </Form>}</Panel>
    <Panel title="Pedidos registrados"><Grid headers={['Pedido','Cliente','Data','Situação','Total']} rows={orders.map(o => [
      <Link to={'/pedidos/' + o.id}>#{o.id}</Link>, clients.find(c => c.id === o.customerId)?.name ?? o.customerId, date(o.createdAt),status[o.status],money(o.total),
    ])} /></Panel>
  </>}</Content></>;
}

export function OrderDetail({admin}: {admin: boolean}) {
  const {id} = useParams();
  const data = useData(async () => {
    const [details, catalog, clients] = await Promise.all([api<{order:Order; items:Item[]}>('/orders/' + id), products(), customers()]);
    return {...details, catalog, clients};
  }, id);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  async function change(action: string) {
    setPending(true); setError('');
    try { await api('/orders/' + id + '/' + action, {}); data.refresh(); }
    catch (error) { setError(error instanceof Error ? error.message : 'Falha ao atualizar.'); }
    finally { setPending(false); }
  }
  return <><Link to="/pedidos">Todos os pedidos</Link><h1>Pedido #{id}</h1><Content data={data}>{({order, items, catalog, clients}) =>
    <Panel title={status[order.status]}>
      <p>{clients.find(c => c.id === order.customerId)?.name} · {date(order.createdAt)}</p>
      <Grid headers={['Produto','Quantidade','Preço unitário']} rows={items.map(item => [
        catalog.find(p => p.id === item.productId)?.name ?? item.productId,item.quantity,money(item.unitPrice),
      ])} />
      <p className="total">Total <strong>{money(order.total)}</strong></p>
      {error && <p className="alert alert-danger" role="alert">{error}</p>}
      <div className="d-flex gap-2 flex-wrap">
        {order.status === 'DRAFT' && <button className="btn btn-primary" disabled={pending} onClick={() => void change('confirm')}>Confirmar pedido</button>}
        {admin && order.status !== 'CANCELED' && <button className="btn btn-outline-danger" disabled={pending} onClick={() => {
          if (window.confirm('Cancelar este pedido? Se já confirmado, as unidades voltarão ao estoque.')) void change('cancel');
        }}>Cancelar pedido</button>}
      </div>
    </Panel>}</Content></>;
}
