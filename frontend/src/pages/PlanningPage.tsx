import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { createBudget, listBudgets, updateBudget } from '../api/budgets'
import { listCategories } from '../api/categories'
import { getByCategory } from '../api/reports'
import { getApiErrorMessage } from '../api/errors'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState } from '../components/ui/Feedback'
import { Input } from '../components/ui/Input'
import { MonthSelector } from '../components/ui/MonthSelector'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { PageHeader } from '../components/layout/PageHeader'
import { currentRef, formatCurrency, formatMonthLabel, refToParts } from '../lib/format'
import type { Budget } from '../types/budget'
import type { Category } from '../types/category'
import type { ByCategoryItem } from '../types/report'

/**
 * Planejamento (`/planejamento`): cadastro e edicao de orcamentos mensais por categoria,
 * com indicador de gasto realizado vs. limite. Consome `GET/POST/PUT /budgets`,
 * `GET /categories` (para o seletor) e `GET /reports/by-category` (gasto do mes).
 */
export default function PlanningPage() {
  const [ref, setRef] = useState(currentRef)
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [spentByCategory, setSpentByCategory] = useState<ByCategoryItem[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [categoryId, setCategoryId] = useState('')
  const [limite, setLimite] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<string | null>(null)
  const [editLimite, setEditLimite] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [budgetList, spent] = await Promise.all([listBudgets(ref), getByCategory(ref)])
      setBudgets(budgetList)
      setSpentByCategory(spent)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Nao foi possivel carregar o planejamento.'))
    } finally {
      setLoading(false)
    }
  }, [ref])

  useEffect(() => {
    const run = async () => {
      await load()
    }
    void run()
  }, [load])

  useEffect(() => {
    let active = true
    listCategories('DESPESA')
      .then((data) => active && setCategories(data))
      .catch(() => {
        /* o seletor apenas fica vazio se as categorias falharem */
      })
    return () => {
      active = false
    }
  }, [])

  /** Gasto realizado de uma categoria no mes (0 quando nao ha lancamentos). */
  function spentFor(catId: string): number {
    return spentByCategory.find((item) => item.categoryId === catId)?.total ?? 0
  }

  /** Categorias ainda sem orcamento no mes, disponiveis para um novo cadastro. */
  const availableCategories = categories.filter(
    (cat) => !budgets.some((budget) => budget.categoryId === cat.id),
  )

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!categoryId || !limite) return
    setFormError(null)
    setSaving(true)
    const { mes, ano } = refToParts(ref)
    try {
      await createBudget({ categoryId, limite: Number(limite), mes, ano })
      setCategoryId('')
      setLimite('')
      await load()
    } catch (err) {
      setFormError(getApiErrorMessage(err, 'Nao foi possivel criar o orcamento.'))
    } finally {
      setSaving(false)
    }
  }

  function startEdit(budget: Budget) {
    setEditingId(budget.id)
    setEditLimite(String(budget.limite))
    setFormError(null)
  }

  async function handleUpdate(budget: Budget) {
    if (!editLimite) return
    setSaving(true)
    setFormError(null)
    try {
      await updateBudget(budget.id, {
        categoryId: budget.categoryId,
        limite: Number(editLimite),
        mes: budget.mes,
        ano: budget.ano,
      })
      setEditingId(null)
      await load()
    } catch (err) {
      setFormError(getApiErrorMessage(err, 'Nao foi possivel atualizar o orcamento.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Planejamento"
        description={`Orcamentos de ${formatMonthLabel(ref)}`}
        actions={<MonthSelector value={ref} onChange={setRef} />}
      />

      <Card className="mb-6">
        <h2 className="mb-4 text-sm font-medium text-slate-700">Novo orcamento</h2>
        <form className="flex flex-wrap items-end gap-4" onSubmit={handleCreate}>
          <Select
            id="categoria"
            label="Categoria"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            required
          >
            <option value="">Selecione...</option>
            {availableCategories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.nome}
              </option>
            ))}
          </Select>
          <Input
            id="limite"
            label="Limite (R$)"
            type="number"
            min="0.01"
            step="0.01"
            value={limite}
            onChange={(e) => setLimite(e.target.value)}
            required
          />
          <Button type="submit" disabled={saving || !categoryId || !limite}>
            {saving ? 'Salvando...' : 'Adicionar'}
          </Button>
        </form>
        {formError && (
          <p role="alert" className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {formError}
          </p>
        )}
      </Card>

      {loading && <Spinner label="Carregando orcamentos..." />}
      {error && !loading && <ErrorState message={error} />}
      {!loading && !error && budgets.length === 0 && (
        <EmptyState message="Nenhum orcamento cadastrado para este mes." />
      )}

      {!loading && !error && budgets.length > 0 && (
        <div className="flex flex-col gap-3">
          {budgets.map((budget) => {
            const spent = spentFor(budget.categoryId)
            const pct = budget.limite > 0 ? (spent / budget.limite) * 100 : 0
            const over = spent > budget.limite
            return (
              <Card key={budget.id}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium text-slate-900">{budget.categoryNome}</span>
                  {editingId === budget.id ? (
                    <div className="flex items-end gap-2">
                      <Input
                        id={`edit-${budget.id}`}
                        label="Limite (R$)"
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={editLimite}
                        onChange={(e) => setEditLimite(e.target.value)}
                      />
                      <Button type="button" onClick={() => handleUpdate(budget)} disabled={saving}>
                        Salvar
                      </Button>
                      <Button
                        type="button"
                        onClick={() => setEditingId(null)}
                        className="bg-slate-200 text-slate-700 hover:bg-slate-300"
                      >
                        Cancelar
                      </Button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3 text-sm">
                      <span className={over ? 'text-red-600' : 'text-slate-600'}>
                        {formatCurrency(spent)} / {formatCurrency(budget.limite)}
                      </span>
                      <button
                        type="button"
                        onClick={() => startEdit(budget)}
                        className="font-medium text-slate-700 underline"
                      >
                        Editar
                      </button>
                    </div>
                  )}
                </div>
                <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full ${over ? 'bg-red-500' : 'bg-emerald-500'}`}
                    style={{ width: `${Math.min(pct, 100)}%` }}
                  />
                </div>
                <p className="mt-1 text-xs text-slate-400">{pct.toFixed(0)}% utilizado</p>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}
