package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.dto.ByCategoryItem;
import com.goodfunds.dto.MonthlyEntry;
import com.goodfunds.dto.SummaryResponse;
import com.goodfunds.exception.InvalidTransactionFilterException;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.projection.CategoryAmount;
import com.goodfunds.repository.projection.MonthAmount;
import com.goodfunds.repository.projection.TipoAmount;
import com.goodfunds.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    /**
     * Teto da amplitude do intervalo de {@code GET /reports/evolution}. A serie e materializada
     * mes a mes em memoria, entao um range sem limite seria um vetor de uso excessivo de
     * memoria/CPU. 36 meses (3 anos) cobre os casos de dashboard com folga.
     */
    static final int MAX_RANGE_MONTHS = 36;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final Clock clock;

    public ReportService(TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository,
                         BudgetRepository budgetRepository,
                         Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID userId, YearMonth ref) {
        if (ref == null) {
            ref = YearMonth.now(clock);
        }
        LocalDate start = ref.atDay(1);
        LocalDate end = ref.atEndOfMonth();

        List<TipoAmount> tipoAmounts = transactionRepository.sumByTipoAndPeriod(userId, start, end);

        BigDecimal receitas = tipoAmounts.stream()
                .filter(a -> a.tipo() == TipoCategoria.RECEITA)
                .map(TipoAmount::total)
                .findFirst()
                .map(MoneyUtils::scale)
                .orElse(MoneyUtils.zero());

        BigDecimal despesas = tipoAmounts.stream()
                .filter(a -> a.tipo() == TipoCategoria.DESPESA)
                .map(TipoAmount::total)
                .findFirst()
                .map(MoneyUtils::scale)
                .orElse(MoneyUtils.zero());

        BigDecimal orcado = MoneyUtils.scale(
                budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, ref.getYear(), ref.getMonthValue()));

        BigDecimal saldo = MoneyUtils.scale(receitas.subtract(despesas));
        BigDecimal percentual = calcPercentualOrcadoUsado(despesas, orcado);

        return new SummaryResponse(ref, receitas, despesas, orcado, saldo, percentual);
    }

    private static BigDecimal calcPercentualOrcadoUsado(BigDecimal despesas, BigDecimal orcado) {
        if (orcado.compareTo(BigDecimal.ZERO) == 0) {
            return MoneyUtils.zero();
        }
        return despesas.multiply(new BigDecimal("100"))
                .divide(orcado, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<ByCategoryItem> byCategory(UUID userId, YearMonth ref) {
        LocalDate start = ref.atDay(1);
        LocalDate end = ref.atEndOfMonth();

        List<CategoryAmount> amounts = transactionRepository.sumByCategoryAndPeriod(userId, start, end);

        Map<UUID, Category> categories = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return amounts.stream()
                .filter(a -> categories.containsKey(a.categoryId()))
                .map(a -> {
                    Category cat = categories.get(a.categoryId());
                    return new ByCategoryItem(cat.getId(), cat.getNome(), cat.getTipo(), MoneyUtils.scale(a.total()));
                })
                .sorted(Comparator.comparing(ByCategoryItem::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyEntry> evolution(UUID userId, YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw new InvalidTransactionFilterException("from deve ser menor ou igual a to");
        }
        long meses = ChronoUnit.MONTHS.between(from, to) + 1;
        if (meses > MAX_RANGE_MONTHS) {
            throw new InvalidTransactionFilterException(
                    "intervalo nao pode exceder " + MAX_RANGE_MONTHS + " meses");
        }

        LocalDate start = from.atDay(1);
        LocalDate end = to.atEndOfMonth();

        List<MonthAmount> amounts = transactionRepository.sumByMonthAndTipo(userId, start, end);
        Map<YearMonth, BigDecimal> receitas = toYearMonthMap(amounts, TipoCategoria.RECEITA);
        Map<YearMonth, BigDecimal> despesas = toYearMonthMap(amounts, TipoCategoria.DESPESA);

        List<MonthlyEntry> entries = new ArrayList<>();
        YearMonth current = from;
        while (!current.isAfter(to)) {
            BigDecimal r = receitas.getOrDefault(current, MoneyUtils.zero());
            BigDecimal d = despesas.getOrDefault(current, MoneyUtils.zero());
            entries.add(new MonthlyEntry(current, r, d));
            current = current.plusMonths(1);
        }
        return entries;
    }

    private static Map<YearMonth, BigDecimal> toYearMonthMap(List<MonthAmount> amounts, TipoCategoria tipo) {
        return amounts.stream()
                .filter(a -> a.tipo() == tipo)
                .collect(Collectors.toMap(
                        a -> YearMonth.of(a.year(), a.month()),
                        a -> MoneyUtils.scale(a.total())));
    }
}
