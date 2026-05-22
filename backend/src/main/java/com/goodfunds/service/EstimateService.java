package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.dto.CategoryEstimate;
import com.goodfunds.dto.EstimateResponse;
import com.goodfunds.dto.EstimateTotals;
import com.goodfunds.repository.CategoryAmount;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Engine de estimativas: projeta o gasto/receita do mes corrente por categoria a partir
 * do historico dos ultimos meses fechados.
 *
 * <ul>
 *   <li><b>media</b>: soma dos lancamentos da categoria nos ultimos 3 meses fechados
 *       dividida por 3 (meses sem lancamento contam como zero).</li>
 *   <li><b>realizado</b>: total ja lancado no mes corrente ate a data de referencia.</li>
 *   <li><b>projecao</b>: extrapolacao do realizado parcial para o mes inteiro,
 *       {@code realizado * (diasNoMes / diasDecorridos)}.</li>
 * </ul>
 */
@Service
public class EstimateService {

    private static final int MESES_FECHADOS = 3;
    private static final BigDecimal DIVISOR_MEDIA = BigDecimal.valueOf(MESES_FECHADOS);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public EstimateService(TransactionRepository transactionRepository,
                           CategoryRepository categoryRepository,
                           Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EstimateResponse estimate(UUID userId) {
        LocalDate hoje = LocalDate.now(clock);
        YearMonth mesAtual = YearMonth.from(hoje);

        LocalDate inicioFechados = mesAtual.minusMonths(MESES_FECHADOS).atDay(1);
        LocalDate fimFechados = mesAtual.minusMonths(1).atEndOfMonth();
        LocalDate inicioMesAtual = mesAtual.atDay(1);

        Map<UUID, BigDecimal> somaFechados =
                toMap(transactionRepository.sumByCategoryAndPeriod(userId, inicioFechados, fimFechados));
        Map<UUID, BigDecimal> somaAtual =
                toMap(transactionRepository.sumByCategoryAndPeriod(userId, inicioMesAtual, hoje));

        int diasNoMes = mesAtual.lengthOfMonth();
        int diasDecorridos = hoje.getDayOfMonth();

        Set<UUID> categoriasAtivas = new LinkedHashSet<>();
        categoriasAtivas.addAll(somaFechados.keySet());
        categoriasAtivas.addAll(somaAtual.keySet());

        Map<UUID, Category> categorias = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<CategoryEstimate> itens = categoriasAtivas.stream()
                .map(categorias::get)
                .filter(category -> category != null)
                .sorted(Comparator.comparing(Category::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(category -> toCategoryEstimate(category, somaFechados, somaAtual, diasNoMes, diasDecorridos))
                .toList();

        return new EstimateResponse(mesAtual, diasNoMes, diasDecorridos, consolidar(itens), itens);
    }

    private CategoryEstimate toCategoryEstimate(Category category,
                                                Map<UUID, BigDecimal> somaFechados,
                                                Map<UUID, BigDecimal> somaAtual,
                                                int diasNoMes,
                                                int diasDecorridos) {
        BigDecimal media = media(somaFechados.get(category.getId()));
        BigDecimal realizado = scale(somaAtual.get(category.getId()));
        BigDecimal projecao = projetar(realizado, diasNoMes, diasDecorridos);
        return new CategoryEstimate(category.getId(), category.getNome(), category.getTipo(),
                media, realizado, projecao);
    }

    private BigDecimal media(BigDecimal totalFechados) {
        if (totalFechados == null) {
            return zero();
        }
        return totalFechados.divide(DIVISOR_MEDIA, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal projetar(BigDecimal realizado, int diasNoMes, int diasDecorridos) {
        return realizado
                .multiply(BigDecimal.valueOf(diasNoMes))
                .divide(BigDecimal.valueOf(diasDecorridos), 2, RoundingMode.HALF_UP);
    }

    private EstimateTotals consolidar(List<CategoryEstimate> itens) {
        BigDecimal media = zero();
        BigDecimal realizado = zero();
        BigDecimal projecao = zero();
        for (CategoryEstimate item : itens) {
            media = media.add(item.media());
            realizado = realizado.add(item.realizado());
            projecao = projecao.add(item.projecao());
        }
        return new EstimateTotals(media, realizado, projecao);
    }

    private static Map<UUID, BigDecimal> toMap(List<CategoryAmount> amounts) {
        return amounts.stream()
                .collect(Collectors.toMap(CategoryAmount::categoryId, CategoryAmount::total));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
