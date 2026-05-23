package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.dto.ByCategoryItem;
import com.goodfunds.dto.MonthlyEntry;
import com.goodfunds.exception.InvalidTransactionFilterException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.projection.CategoryAmount;
import com.goodfunds.repository.projection.MonthAmount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public ReportService(TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
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
                    return new ByCategoryItem(cat.getId(), cat.getNome(), scale(a.total()));
                })
                .sorted(Comparator.comparing(ByCategoryItem::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyEntry> evolution(UUID userId, YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw new InvalidTransactionFilterException("from deve ser menor ou igual a to");
        }

        LocalDate start = from.atDay(1);
        LocalDate end = to.atEndOfMonth();

        Map<YearMonth, BigDecimal> receitas = toYearMonthMap(
                transactionRepository.sumByMonthAndTipo(userId, start, end, TipoCategoria.RECEITA));
        Map<YearMonth, BigDecimal> despesas = toYearMonthMap(
                transactionRepository.sumByMonthAndTipo(userId, start, end, TipoCategoria.DESPESA));

        List<MonthlyEntry> entries = new ArrayList<>();
        YearMonth current = from;
        while (!current.isAfter(to)) {
            BigDecimal r = receitas.getOrDefault(current, zero());
            BigDecimal d = despesas.getOrDefault(current, zero());
            entries.add(new MonthlyEntry(current, r, d));
            current = current.plusMonths(1);
        }
        return entries;
    }

    private static Map<YearMonth, BigDecimal> toYearMonthMap(List<MonthAmount> amounts) {
        return amounts.stream()
                .collect(Collectors.toMap(
                        a -> YearMonth.of(a.year(), a.month()),
                        a -> scale(a.total())));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
