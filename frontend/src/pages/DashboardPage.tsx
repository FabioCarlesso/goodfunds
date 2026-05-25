import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getSummary } from '../api/reports'
import { getApiErrorMessage } from '../api/errors'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/Feedback'
import { MonthSelector } from '../components/ui/MonthSelector'
import { Spinner } from '../components/ui/Spinner'
import { PageHeader } from '../components/layout/PageHeader'
import { currentRef, formatCurrency, formatMonthLabel } from '../lib/format'
import type { SummaryResponse } from '../types/report'

/** Cor das barras: verde para receitas, vermelho para despesas. */
const BAR_COLORS = ['#059669', '#dc2626']

/**
 * Dashboard (`/dashboard`): visao geral do mes corrente (saldo, receitas, despesas e
 * uso do orcamento) com um grafico simples de receitas vs despesas. Um seletor de mes
 * permite navegar entre periodos consumindo `GET /reports/summary`.
 */
export default function DashboardPage() {
  const [ref, setRef] = useState(currentRef)
  const [summary, setSummary] = useState<SummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await getSummary(ref)
        if (active) setSummary(data)
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Nao foi possivel carregar o resumo do mes.'))
      } finally {
        if (active) setLoading(false)
      }
    }
    void run()
    return () => {
      active = false
    }
  }, [ref])

  const chartData = summary
    ? [
        { nome: 'Receitas', valor: summary.receitas },
        { nome: 'Despesas', valor: summary.despesas },
      ]
    : []

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description={formatMonthLabel(ref)}
        actions={<MonthSelector value={ref} onChange={setRef} />}
      />

      {loading && <Spinner label="Carregando resumo..." />}
      {error && !loading && <ErrorState message={error} />}

      {summary && !loading && !error && (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard label="Saldo" value={summary.saldo} highlight />
            <SummaryCard label="Receitas" value={summary.receitas} />
            <SummaryCard label="Despesas" value={summary.despesas} />
            <Card>
              <p className="text-sm text-slate-500">Orcamento usado</p>
              <p className="mt-1 text-2xl font-bold text-slate-900">
                {summary.percentualOrcadoUsado.toFixed(0)}%
              </p>
              <p className="mt-1 text-xs text-slate-400">
                de {formatCurrency(summary.orcado)} orcados
              </p>
            </Card>
          </div>

          <Card>
            <h2 className="mb-4 text-sm font-medium text-slate-700">Receitas vs Despesas</h2>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <XAxis dataKey="nome" tickLine={false} axisLine={false} />
                  <YAxis tickFormatter={(v) => formatCurrency(v)} width={90} tickLine={false} axisLine={false} />
                  <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                  <Bar dataKey="valor" radius={[4, 4, 0, 0]}>
                    {chartData.map((entry, index) => (
                      <Cell key={entry.nome} fill={BAR_COLORS[index % BAR_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}

/** Cartao de valor monetario do resumo; em `highlight` colore conforme o sinal. */
function SummaryCard({
  label,
  value,
  highlight = false,
}: {
  label: string
  value: number
  highlight?: boolean
}) {
  const valueColor = highlight
    ? value >= 0
      ? 'text-emerald-600'
      : 'text-red-600'
    : 'text-slate-900'
  return (
    <Card>
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${valueColor}`}>{formatCurrency(value)}</p>
    </Card>
  )
}
