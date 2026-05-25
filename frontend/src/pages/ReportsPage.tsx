import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getByCategory, getEvolution } from '../api/reports'
import { getApiErrorMessage } from '../api/errors'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState } from '../components/ui/Feedback'
import { Input } from '../components/ui/Input'
import { Spinner } from '../components/ui/Spinner'
import { PageHeader } from '../components/layout/PageHeader'
import { addMonths, currentRef, formatCurrency } from '../lib/format'
import type { ByCategoryItem, MonthlyEntry } from '../types/report'

/** Paleta ciclica para as fatias do grafico por categoria. */
const PIE_COLORS = ['#2563eb', '#059669', '#dc2626', '#d97706', '#7c3aed', '#0891b2', '#db2777']

/**
 * Relatorios (`/relatorios`): evolucao mensal de receitas vs despesas e distribuicao
 * de gastos por categoria, com filtro de periodo. Consome `GET /reports/evolution`
 * e `GET /reports/by-category`.
 */
export default function ReportsPage() {
  const [from, setFrom] = useState(() => addMonths(currentRef(), -5))
  const [to, setTo] = useState(currentRef)
  const [evolution, setEvolution] = useState<MonthlyEntry[]>([])
  const [byCategory, setByCategory] = useState<ByCategoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const [evo, cats] = await Promise.all([getEvolution(from, to), getByCategory(to)])
        if (!active) return
        setEvolution(evo)
        setByCategory(cats)
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Nao foi possivel carregar os relatorios.'))
      } finally {
        if (active) setLoading(false)
      }
    }
    void run()
    return () => {
      active = false
    }
  }, [from, to])

  const despesasPorCategoria = byCategory.filter((item) => item.tipo === 'DESPESA' && item.total > 0)

  return (
    <div>
      <PageHeader
        title="Relatorios"
        description="Evolucao mensal e distribuicao de gastos por categoria."
        actions={
          <div className="flex items-end gap-2">
            <Input
              id="from"
              label="De"
              type="month"
              value={from}
              max={to}
              onChange={(e) => setFrom(e.target.value)}
            />
            <Input
              id="to"
              label="Ate"
              type="month"
              value={to}
              min={from}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        }
      />

      {loading && <Spinner label="Carregando relatorios..." />}
      {error && !loading && <ErrorState message={error} />}

      {!loading && !error && (
        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="mb-4 text-sm font-medium text-slate-700">
              Evolucao mensal (receitas vs despesas)
            </h2>
            {evolution.length === 0 ? (
              <EmptyState message="Sem dados no periodo selecionado." />
            ) : (
              <div className="h-72 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={evolution}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="ref" tickLine={false} axisLine={false} />
                    <YAxis tickFormatter={(v) => formatCurrency(v)} width={90} tickLine={false} axisLine={false} />
                    <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                    <Legend />
                    <Bar dataKey="receitas" name="Receitas" fill="#059669" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="despesas" name="Despesas" fill="#dc2626" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>

          <Card>
            <h2 className="mb-4 text-sm font-medium text-slate-700">
              Gastos por categoria ({to})
            </h2>
            {despesasPorCategoria.length === 0 ? (
              <EmptyState message="Sem despesas no mes selecionado." />
            ) : (
              <div className="h-72 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={despesasPorCategoria}
                      dataKey="total"
                      nameKey="nome"
                      cx="50%"
                      cy="50%"
                      outerRadius={90}
                    >
                      {despesasPorCategoria.map((entry, index) => (
                        <Cell key={entry.categoryId} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}
