package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BudgetRepository budgetRepository;

    // Congela "hoje" em 2026-05-22 para testes que dependem de mes corrente.
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-22T12:00:00Z"), ZoneOffset.UTC);

    private ReportService reportService;

    private final UUID userId = UUID.randomUUID();
    private User user;
    private Category alimentacao;
    private Category transporte;
    private Category salario;

    @BeforeEach
    void setup() {
        reportService = new ReportService(transactionRepository, categoryRepository, budgetRepository, fixedClock);
        user = User.builder().id(userId).nome("Test").email("t@example.com").senha("hash").build();
        alimentacao = category("Alimentacao", TipoCategoria.DESPESA);
        transporte = category("Transporte", TipoCategoria.DESPESA);
        salario = category("Salario", TipoCategoria.RECEITA);
    }

    // ---------- summary ----------

    @Test
    void summary_computesAllFieldsCorrectly() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByTipoAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of(
                        new TipoAmount(TipoCategoria.RECEITA, new BigDecimal("3000")),
                        new TipoAmount(TipoCategoria.DESPESA, new BigDecimal("1200"))));
        when(budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, 2026, 5))
                .thenReturn(new BigDecimal("2000"));

        SummaryResponse result = reportService.summary(userId, ref);

        assertThat(result.ref()).isEqualTo(ref);
        assertThat(result.receitas()).isEqualByComparingTo("3000.00");
        assertThat(result.despesas()).isEqualByComparingTo("1200.00");
        assertThat(result.orcado()).isEqualByComparingTo("2000.00");
        assertThat(result.saldo()).isEqualByComparingTo("1800.00");
        assertThat(result.percentualOrcadoUsado()).isEqualByComparingTo("60.00");
    }

    @Test
    void summary_noTransactionsNoBudget_returnsAllZeros() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByTipoAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of());
        when(budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, 2026, 5))
                .thenReturn(BigDecimal.ZERO);

        SummaryResponse result = reportService.summary(userId, ref);

        assertThat(result.receitas()).isEqualByComparingTo("0.00");
        assertThat(result.despesas()).isEqualByComparingTo("0.00");
        assertThat(result.orcado()).isEqualByComparingTo("0.00");
        assertThat(result.saldo()).isEqualByComparingTo("0.00");
        assertThat(result.percentualOrcadoUsado()).isEqualByComparingTo("0.00");
    }

    @Test
    void summary_zeroBudget_percentualIsZero() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByTipoAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of(new TipoAmount(TipoCategoria.DESPESA, new BigDecimal("500"))));
        when(budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, 2026, 5))
                .thenReturn(BigDecimal.ZERO);

        SummaryResponse result = reportService.summary(userId, ref);

        assertThat(result.despesas()).isEqualByComparingTo("500.00");
        assertThat(result.percentualOrcadoUsado()).isEqualByComparingTo("0.00");
    }

    @Test
    void summary_onlyDespesas_saldoIsNegative() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByTipoAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of(new TipoAmount(TipoCategoria.DESPESA, new BigDecimal("800"))));
        when(budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, 2026, 5))
                .thenReturn(new BigDecimal("1000"));

        SummaryResponse result = reportService.summary(userId, ref);

        assertThat(result.receitas()).isEqualByComparingTo("0.00");
        assertThat(result.saldo()).isEqualByComparingTo("-800.00");
        assertThat(result.percentualOrcadoUsado()).isEqualByComparingTo("80.00");
    }

    @Test
    void summary_nullRef_defaultsToCurrentMonth() {
        YearMonth currentMonth = YearMonth.of(2026, 5); // fixedClock aponta para 2026-05-22
        when(transactionRepository.sumByTipoAndPeriod(userId, currentMonth.atDay(1), currentMonth.atEndOfMonth()))
                .thenReturn(List.of());
        when(budgetRepository.sumLimiteByUserIdAndAnoAndMes(userId, 2026, 5))
                .thenReturn(BigDecimal.ZERO);

        SummaryResponse result = reportService.summary(userId, null);

        assertThat(result.ref()).isEqualTo(currentMonth);
    }

    // ---------- byCategory ----------

    @Test
    void byCategory_ordersByNameAndExposesTipoAndScale() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByCategoryAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of(
                        new CategoryAmount(transporte.getId(), new BigDecimal("80")),
                        new CategoryAmount(salario.getId(), new BigDecimal("3000")),
                        new CategoryAmount(alimentacao.getId(), new BigDecimal("150.5"))));
        when(categoryRepository.findByUserId(userId))
                .thenReturn(List.of(alimentacao, transporte, salario));

        List<ByCategoryItem> result = reportService.byCategory(userId, ref);

        assertThat(result).extracting(ByCategoryItem::nome)
                .containsExactly("Alimentacao", "Salario", "Transporte");
        assertThat(result.get(0).tipo()).isEqualTo(TipoCategoria.DESPESA);
        assertThat(result.get(0).total()).isEqualByComparingTo("150.50");
        assertThat(result.get(1).tipo()).isEqualTo(TipoCategoria.RECEITA);
        assertThat(result.get(1).total()).isEqualByComparingTo("3000.00");
    }

    @Test
    void byCategory_dropsAmountsWithoutMatchingCategory() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByCategoryAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of(
                        new CategoryAmount(alimentacao.getId(), new BigDecimal("100")),
                        // categoria que nao pertence ao usuario / inexistente no mapa
                        new CategoryAmount(UUID.randomUUID(), new BigDecimal("999"))));
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(alimentacao));

        List<ByCategoryItem> result = reportService.byCategory(userId, ref);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("Alimentacao");
    }

    @Test
    void byCategory_monthWithoutData_returnsEmptyList() {
        YearMonth ref = YearMonth.of(2026, 5);
        when(transactionRepository.sumByCategoryAndPeriod(userId, ref.atDay(1), ref.atEndOfMonth()))
                .thenReturn(List.of());

        assertThat(reportService.byCategory(userId, ref)).isEmpty();
    }

    // ---------- evolution ----------

    @Test
    void evolution_mergesTiposAndZeroFillsMissingMonths() {
        YearMonth from = YearMonth.of(2026, 1);
        YearMonth to = YearMonth.of(2026, 3);
        when(transactionRepository.sumByMonthAndTipo(userId, from.atDay(1), to.atEndOfMonth()))
                .thenReturn(List.of(
                        new MonthAmount(2026, 1, TipoCategoria.RECEITA, new BigDecimal("1000")),
                        new MonthAmount(2026, 1, TipoCategoria.DESPESA, new BigDecimal("200")),
                        // fevereiro sem dados -> zeros
                        new MonthAmount(2026, 3, TipoCategoria.DESPESA, new BigDecimal("350"))));

        List<MonthlyEntry> result = reportService.evolution(userId, from, to);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).ref()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(result.get(0).receitas()).isEqualByComparingTo("1000.00");
        assertThat(result.get(0).despesas()).isEqualByComparingTo("200.00");
        assertThat(result.get(1).ref()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(result.get(1).receitas()).isEqualByComparingTo("0.00");
        assertThat(result.get(1).despesas()).isEqualByComparingTo("0.00");
        assertThat(result.get(2).receitas()).isEqualByComparingTo("0.00");
        assertThat(result.get(2).despesas()).isEqualByComparingTo("350.00");

        // uma unica query ao banco (nao mais uma por tipo)
        verify(transactionRepository, times(1))
                .sumByMonthAndTipo(eq(userId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void evolution_fromAfterTo_throwsInvalidFilter() {
        assertThatThrownBy(() -> reportService.evolution(userId, YearMonth.of(2026, 5), YearMonth.of(2026, 1)))
                .isInstanceOf(InvalidTransactionFilterException.class);
        verify(transactionRepository, never())
                .sumByMonthAndTipo(any(), any(), any());
    }

    @Test
    void evolution_rangeExceedingMax_throwsInvalidFilter() {
        // 37 meses (2026-01 .. 2029-01) excede o teto de 36.
        assertThatThrownBy(() -> reportService.evolution(userId, YearMonth.of(2026, 1), YearMonth.of(2029, 1)))
                .isInstanceOf(InvalidTransactionFilterException.class)
                .hasMessageContaining("36");
        verify(transactionRepository, never())
                .sumByMonthAndTipo(any(), any(), any());
    }

    @Test
    void evolution_rangeAtMaxBoundary_isAccepted() {
        // 36 meses exatos (2026-01 .. 2028-12) — limite permitido.
        YearMonth from = YearMonth.of(2026, 1);
        YearMonth to = YearMonth.of(2028, 12);
        when(transactionRepository.sumByMonthAndTipo(userId, from.atDay(1), to.atEndOfMonth()))
                .thenReturn(List.of());

        List<MonthlyEntry> result = reportService.evolution(userId, from, to);

        assertThat(result).hasSize(36);
        assertThat(result.get(0).ref()).isEqualTo(from);
        assertThat(result.get(35).ref()).isEqualTo(to);
    }

    private Category category(String nome, TipoCategoria tipo) {
        return Category.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .tipo(tipo)
                .user(user)
                .build();
    }
}
