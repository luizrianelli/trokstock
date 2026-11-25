import { useEffect, useState, type FormEvent, type ReactNode } from 'react';

export function useData<T>(load: () => Promise<T>, key = '') {
  const [value, setValue] = useState<T>();
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    setError('');
    setValue(undefined);
    load().then(data => { if (active) setValue(data); })
      .catch(error => { if (active) setError(error.message); });
    return () => { active = false; };
  }, [key, version]);
  return {value, error, refresh: () => setVersion(version => version + 1)};
}

export function Content<T>({data, children}: {
  data: {value?: T; error: string}; children: (data: T) => ReactNode;
}) {
  if (data.error) return <p className="alert alert-danger" role="alert">{data.error}</p>;
  if (data.value === undefined) return <p role="status">Carregando…</p>;
  return <>{children(data.value)}</>;
}

export function Form({children, submit, label}: {
  children: ReactNode; submit: (form: FormData) => Promise<unknown>; label: string;
}) {
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setPending(true);
    setError('');
    try {
      await submit(new FormData(form));
      form.reset();
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Falha na operação.');
    } finally {
      setPending(false);
    }
  }
  return <form onSubmit={send}>
    {error && <p className="alert alert-danger" role="alert">{error}</p>}
    <fieldset disabled={pending}>
      <div className="fields">{children}</div>
      <button className="btn btn-primary mt-3">{pending ? 'Aguarde…' : label}</button>
    </fieldset>
  </form>;
}

export function Field({label, children}: {label: string; children: ReactNode}) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

export function Panel({title, children}: {title: string; children: ReactNode}) {
  return <section className="panel"><h2>{title}</h2>{children}</section>;
}

export function Grid({headers, rows}: {headers: string[]; rows: ReactNode[][]}) {
  if (!rows.length) return <p className="empty">Nenhum registro encontrado.</p>;
  return <div className="table-responsive"><table className="table align-middle mb-0">
    <thead><tr>{headers.map(label => <th scope="col" key={label}>{label}</th>)}</tr></thead>
    <tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, column) => <td key={column}>{cell}</td>)}</tr>)}</tbody>
  </table></div>;
}

export const money = (value: number) => new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'}).format(value);
export const date = (value: string) => new Intl.DateTimeFormat('pt-BR', {dateStyle:'short', timeStyle:'short'}).format(new Date(value));
