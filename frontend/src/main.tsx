import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import './style.css';
import { api, ApiError } from './api';
import { Customers, Dashboard, Inventory, OrderDetail, Orders, Products } from './pages';
import { Field, Form } from './ui';

type Session = {username: string; roles: string[]};
const links = [['/', 'Visão geral'], ['/produtos','Produtos'], ['/clientes','Clientes'], ['/estoque','Estoque'], ['/pedidos','Pedidos']];

function App() {
  const [session, setSession] = useState<Session | null>();
  const [error, setError] = useState('');
  async function reload() {
    setError('');
    try { setSession(await api<Session>('/auth/me')); }
    catch (error) {
      setSession(null);
      if (!(error instanceof ApiError && error.status === 401)) setError('Falha ao conectar com a API.');
    }
  }
  useEffect(() => { void reload(); }, []);
  if (session === undefined) return <main className="login"><p role="status">Verificando sessão…</p></main>;
  if (!session) return <main className="login">
    <div className="login-intro"><span className="brand">Trok&Stock</span><h1>Pedidos e estoque em um só lugar</h1></div>
    <div className="panel login-form"><h2>Entrar</h2>
      {error && <p className="alert alert-danger" role="alert">{error}</p>}
      <Form label="Acessar Trok&Stock" submit={async form => {
        await api('/auth/login', new URLSearchParams({username:String(form.get('username')), password:String(form.get('password'))}));
        await reload();
      }}>
        <Field label="Usuário"><input className="form-control" name="username" autoComplete="username" required /></Field>
        <Field label="Senha"><input className="form-control" name="password" type="password" autoComplete="current-password" required /></Field>
      </Form>
    </div>
  </main>;
  const admin = session.roles.includes('ROLE_ADMIN');
  return <div className="workspace">
    <aside className="sidebar">
      <NavLink className="brand" to="/">Trok&Stock</NavLink>
      <nav aria-label="Menu principal">{links.map(([url, title]) => <NavLink end={url === '/'} key={url} to={url}>{title}</NavLink>)}</nav>
    </aside>
    <main className="work">
      <header className="topbar"><span>{session.username} <small>{admin ? 'Administrador' : 'Operador'}</small></span>
        <button className="btn btn-outline-secondary btn-sm" onClick={async () => {
          try { await api('/auth/logout', {}); setSession(null); }
          catch (error) {
            if (error instanceof ApiError && error.status === 401) setSession(null);
            else setError('Falha ao encerrar a sessão. Tente novamente.');
          }
        }}>Sair</button></header>
      {error && <p className="alert alert-danger" role="alert">{error}</p>}
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/produtos" element={<Products />} />
        <Route path="/clientes" element={<Customers />} />
        <Route path="/estoque" element={<Inventory admin={admin} />} />
        <Route path="/pedidos" element={<Orders />} />
        <Route path="/pedidos/:id" element={<OrderDetail admin={admin} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </main>
  </div>;
}

createRoot(document.getElementById('root')!).render(<BrowserRouter><App /></BrowserRouter>);
