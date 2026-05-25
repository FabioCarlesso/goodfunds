import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getInvoice } from '../api/invoices'
import { getApiErrorMessage } from '../api/errors'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState } from '../components/ui/Feedback'
import { Spinner } from '../components/ui/Spinner'
import { StatusBadge } from '../components/ui/StatusBadge'
import { PageHeader } from '../components/layout/PageHeader'
import { formatCurrency, formatDate } from '../lib/format'
import type { InvoiceDetail } from '../types/invoice'

/**
 * Detalhe da fatura (`/faturas/:id`): metadados da fatura e a lista de transacoes
 * geradas no processamento. Consome `GET /invoices/{id}`.
 */
export default function InvoiceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let active = true
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await getInvoice(id)
        if (active) setInvoice(data)
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Nao foi possivel carregar a fatura.'))
      } finally {
        if (active) setLoading(false)
      }
    }
    void run()
    return () => {
      active = false
    }
  }, [id])

  const transactions = invoice?.transactions ?? []

  return (
    <div>
      <PageHeader
        title="Detalhe da fatura"
        actions={
          <Link to="/faturas" className="text-sm font-medium text-slate-700 underline">
            ← Voltar
          </Link>
        }
      />

      {loading && <Spinner label="Carregando fatura..." />}
      {error && !loading && <ErrorState message={error} />}

      {invoice && !loading && !error && (
        <div className="flex flex-col gap-6">
          <Card>
            <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
              <div>
                <dt className="text-slate-500">Arquivo</dt>
                <dd className="font-medium text-slate-900">{invoice.arquivo}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Origem</dt>
                <dd className="font-medium text-slate-900">{invoice.origem}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Status</dt>
                <dd>
                  <StatusBadge status={invoice.status} />
                </dd>
              </div>
              <div>
                <dt className="text-slate-500">Mes ref.</dt>
                <dd className="font-medium text-slate-900">{invoice.mesReferencia ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Total</dt>
                <dd className="font-medium text-slate-900">
                  {invoice.totalValor != null ? formatCurrency(invoice.totalValor) : '—'}
                </dd>
              </div>
            </dl>
          </Card>

          <section>
            <h2 className="mb-3 text-sm font-medium text-slate-700">
              Transacoes geradas ({transactions.length})
            </h2>
            {transactions.length === 0 ? (
              <EmptyState message="Nenhuma transacao gerada para esta fatura." />
            ) : (
              <Card className="overflow-x-auto p-0">
                <table className="w-full text-left text-sm">
                  <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">Data</th>
                      <th className="px-4 py-3">Descricao</th>
                      <th className="px-4 py-3">Categoria</th>
                      <th className="px-4 py-3 text-right">Valor</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((tx) => (
                      <tr key={tx.id} className="border-b border-slate-100 last:border-0">
                        <td className="px-4 py-3 text-slate-600">{formatDate(tx.data)}</td>
                        <td className="px-4 py-3 text-slate-900">{tx.descricao}</td>
                        <td className="px-4 py-3 text-slate-600">{tx.categoryNome}</td>
                        <td className="px-4 py-3 text-right text-slate-900">
                          {formatCurrency(tx.valor)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            )}
          </section>
        </div>
      )}
    </div>
  )
}
