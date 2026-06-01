import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { deleteInvoice, listInvoices, processInvoice, uploadInvoice } from '../api/invoices'
import { getApiErrorMessage } from '../api/errors'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState } from '../components/ui/Feedback'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { StatusBadge } from '../components/ui/StatusBadge'
import { PageHeader } from '../components/layout/PageHeader'
import { formatCurrency, formatDate } from '../lib/format'
import type { OrigemFatura } from '../types/common'
import type { Invoice } from '../types/invoice'

const ORIGENS: OrigemFatura[] = ['NUBANK', 'ITAU', 'OUTROS']

/**
 * Faturas (`/faturas`): upload de PDF e listagem das faturas importadas. Cada linha
 * leva ao detalhe (`/faturas/:id`) com as transacoes geradas. Consome
 * `GET /invoices` e `POST /invoices/upload`.
 */
export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [file, setFile] = useState<File | null>(null)
  const [origem, setOrigem] = useState<OrigemFatura | ''>('')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [processingId, setProcessingId] = useState<string | null>(null)
  const [processError, setProcessError] = useState<string | null>(null)

  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setInvoices(await listInvoices())
    } catch (err) {
      setError(getApiErrorMessage(err, 'Nao foi possivel carregar as faturas.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const run = async () => {
      await load()
    }
    void run()
  }, [load])

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return
    setUploadError(null)
    setUploading(true)
    try {
      await uploadInvoice(file, origem || undefined)
      setFile(null)
      setOrigem('')
      if (fileInputRef.current) fileInputRef.current.value = ''
      await load()
    } catch (err) {
      setUploadError(getApiErrorMessage(err, 'Nao foi possivel enviar a fatura.'))
    } finally {
      setUploading(false)
    }
  }

  async function handleProcess(id: string) {
    setProcessError(null)
    setProcessingId(id)
    try {
      await processInvoice(id)
      await load()
    } catch (err) {
      setProcessError(getApiErrorMessage(err, 'Nao foi possivel processar a fatura.'))
    } finally {
      setProcessingId(null)
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm('Excluir esta fatura e as transacoes geradas a partir dela?')) {
      return
    }
    setDeleteError(null)
    setDeletingId(id)
    try {
      await deleteInvoice(id)
      await load()
    } catch (err) {
      setDeleteError(getApiErrorMessage(err, 'Nao foi possivel excluir a fatura.'))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div>
      <PageHeader title="Faturas" description="Importe e acompanhe suas faturas em PDF." />

      <Card className="mb-6">
        <h2 className="mb-4 text-sm font-medium text-slate-700">Importar fatura</h2>
        <form className="flex flex-wrap items-end gap-4" onSubmit={handleUpload}>
          <div className="flex flex-col gap-1 text-left">
            <label htmlFor="file" className="text-sm font-medium text-slate-700">
              Arquivo PDF
            </label>
            <input
              id="file"
              ref={fileInputRef}
              type="file"
              accept="application/pdf,.pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              className="text-sm text-slate-700 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-slate-700 hover:file:bg-slate-200"
            />
          </div>
          <Select
            id="origem"
            label="Origem (opcional)"
            value={origem}
            onChange={(e) => setOrigem(e.target.value as OrigemFatura | '')}
          >
            <option value="">Detectar</option>
            {ORIGENS.map((o) => (
              <option key={o} value={o}>
                {o}
              </option>
            ))}
          </Select>
          <Button type="submit" disabled={!file || uploading}>
            {uploading ? 'Enviando...' : 'Enviar'}
          </Button>
        </form>
        {uploadError && (
          <p role="alert" className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {uploadError}
          </p>
        )}
      </Card>

      {loading && <Spinner label="Carregando faturas..." />}
      {error && !loading && <ErrorState message={error} />}
      {!loading && !error && invoices.length === 0 && (
        <EmptyState message="Nenhuma fatura importada ainda." />
      )}

      {processError && !loading && (
        <p role="alert" className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {processError}
        </p>
      )}
      {deleteError && !loading && (
        <p role="alert" className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {deleteError}
        </p>
      )}

      {!loading && !error && invoices.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Arquivo</th>
                <th className="px-4 py-3">Origem</th>
                <th className="px-4 py-3">Mes ref.</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Total</th>
                <th className="px-4 py-3">Importada em</th>
                <th className="px-4 py-3 text-right">Acoes</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id} className="border-b border-slate-100 last:border-0">
                  <td className="px-4 py-3">
                    <Link
                      to={`/faturas/${invoice.id}`}
                      className="font-medium text-slate-900 underline"
                    >
                      {invoice.arquivo}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{invoice.origem}</td>
                  <td className="px-4 py-3 text-slate-600">{invoice.mesReferencia ?? '—'}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={invoice.status} />
                  </td>
                  <td className="px-4 py-3 text-right text-slate-900">
                    {invoice.totalValor != null ? formatCurrency(invoice.totalValor) : '—'}
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {formatDate(invoice.createdAt.slice(0, 10))}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {invoice.status !== 'PROCESSADA' && (
                        <Button
                          type="button"
                          onClick={() => handleProcess(invoice.id)}
                          disabled={processingId === invoice.id || deletingId === invoice.id}
                        >
                          {processingId === invoice.id
                            ? 'Processando...'
                            : invoice.status === 'ERRO'
                              ? 'Reprocessar'
                              : 'Processar'}
                        </Button>
                      )}
                      <Button
                        type="button"
                        onClick={() => handleDelete(invoice.id)}
                        disabled={deletingId === invoice.id || processingId === invoice.id}
                        className="bg-red-600 hover:bg-red-500"
                      >
                        {deletingId === invoice.id ? 'Excluindo...' : 'Excluir'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  )
}
