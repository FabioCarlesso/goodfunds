package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.CategoryEstimate;
import com.goodfunds.dto.EstimateResponse;
import com.goodfunds.repository.CategoryAmount;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {

    // Relogio fixo em 22/05/2026: maio tem 31 dias e 22 ja decorreram.
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate FECHADOS_INICIO = LocalDate.of(2026, 2, 1);
    private static final LocalDate FECHADOS_FIM = LocalDate.of(2026, 4, 30);
    private static final LocalDate ATUAL_INICIO = LocalDate.of(2026, 5, 1);
    private static final LocalDate HOJE = LocalDate.of(2026, 5, 22);

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;

    private EstimateService estimateService;

    private final UUID userId = UUID.randomUUID();
    private User user;
    private Category alimentacao;
    private Category transporte;
    private Category lazer;

    @BeforeEach
    void setup() {
        estimateService = new EstimateService(transactionRepository, categoryRepository, FIXED_CLOCK);
        user = User.builder().id(userId).nome("Test").email("t@example.com").senha("hash").build();
        alimentacao = category("Alimentacao");
        transporte = category("Transporte");
        lazer = category("Lazer");
    }

    @Test
    void estimate_userWithoutHistory_returnsEmptyAndZeroTotals() {
        when(transactionRepository.sumByCategoryAndPeriod(userId, FECHADOS_INICIO, FECHADOS_FIM))
                .thenReturn(List.of());
        when(transactionRepository.sumByCategoryAndPeriod(userId, ATUAL_INICIO, HOJE))
                .thenReturn(List.of());

        EstimateResponse response = estimateService.estimate(userId);

        assertThat(response.ref()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(response.diasNoMes()).isEqualTo(31);
        assertThat(response.diasDecorridos()).isEqualTo(22);
        assertThat(response.categorias()).isEmpty();
        assertThat(response.consolidado().media()).isEqualByComparingTo("0.00");
        assertThat(response.consolidado().realizado()).isEqualByComparingTo("0.00");
        assertThat(response.consolidado().projecao()).isEqualByComparingTo("0.00");
    }

    @Test
    void estimate_partialHistory_averagesOverThreeMonths() {
        // Lancamentos em apenas 1 dos 3 meses fechados: 300 / 3 = 100.
        when(transactionRepository.sumByCategoryAndPeriod(userId, FECHADOS_INICIO, FECHADOS_FIM))
                .thenReturn(List.of(new CategoryAmount(alimentacao.getId(), new BigDecimal("300.00"))));
        when(transactionRepository.sumByCategoryAndPeriod(userId, ATUAL_INICIO, HOJE))
                .thenReturn(List.of());
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(alimentacao));

        EstimateResponse response = estimateService.estimate(userId);

        assertThat(response.categorias()).hasSize(1);
        CategoryEstimate item = response.categorias().get(0);
        assertThat(item.categoryNome()).isEqualTo("Alimentacao");
        assertThat(item.media()).isEqualByComparingTo("100.00");
        assertThat(item.realizado()).isEqualByComparingTo("0.00");
        assertThat(item.projecao()).isEqualByComparingTo("0.00");
        assertThat(response.consolidado().media()).isEqualByComparingTo("100.00");
    }

    @Test
    void estimate_completeHistory_projectsPartialSpend() {
        // 900 / 3 = 300 de media; realizado 220 extrapolado: 220 * 31 / 22 = 310.
        when(transactionRepository.sumByCategoryAndPeriod(userId, FECHADOS_INICIO, FECHADOS_FIM))
                .thenReturn(List.of(new CategoryAmount(alimentacao.getId(), new BigDecimal("900.00"))));
        when(transactionRepository.sumByCategoryAndPeriod(userId, ATUAL_INICIO, HOJE))
                .thenReturn(List.of(new CategoryAmount(alimentacao.getId(), new BigDecimal("220.00"))));
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(alimentacao));

        EstimateResponse response = estimateService.estimate(userId);

        assertThat(response.categorias()).hasSize(1);
        CategoryEstimate item = response.categorias().get(0);
        assertThat(item.media()).isEqualByComparingTo("300.00");
        assertThat(item.realizado()).isEqualByComparingTo("220.00");
        assertThat(item.projecao()).isEqualByComparingTo("310.00");
        assertThat(response.consolidado().media()).isEqualByComparingTo("300.00");
        assertThat(response.consolidado().realizado()).isEqualByComparingTo("220.00");
        assertThat(response.consolidado().projecao()).isEqualByComparingTo("310.00");
    }

    @Test
    void estimate_multipleCategories_ordersByNameAndConsolidatesTotals() {
        when(transactionRepository.sumByCategoryAndPeriod(userId, FECHADOS_INICIO, FECHADOS_FIM))
                .thenReturn(List.of(
                        new CategoryAmount(alimentacao.getId(), new BigDecimal("900.00")),
                        new CategoryAmount(transporte.getId(), new BigDecimal("300.00"))));
        when(transactionRepository.sumByCategoryAndPeriod(userId, ATUAL_INICIO, HOJE))
                .thenReturn(List.of(
                        new CategoryAmount(alimentacao.getId(), new BigDecimal("220.00")),
                        new CategoryAmount(lazer.getId(), new BigDecimal("50.00"))));
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(alimentacao, transporte, lazer));

        EstimateResponse response = estimateService.estimate(userId);

        assertThat(response.categorias())
                .extracting(CategoryEstimate::categoryNome)
                .containsExactly("Alimentacao", "Lazer", "Transporte");

        // Lazer aparece por ter lancamento no mes corrente, mesmo sem historico fechado.
        CategoryEstimate lazerItem = response.categorias().get(1);
        assertThat(lazerItem.media()).isEqualByComparingTo("0.00");
        assertThat(lazerItem.realizado()).isEqualByComparingTo("50.00");
        assertThat(lazerItem.projecao()).isEqualByComparingTo("70.45"); // 50 * 31 / 22

        assertThat(response.consolidado().media()).isEqualByComparingTo("400.00");
        assertThat(response.consolidado().realizado()).isEqualByComparingTo("270.00");
        assertThat(response.consolidado().projecao()).isEqualByComparingTo("380.45");
    }

    private Category category(String nome) {
        return Category.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .tipo(TipoCategoria.DESPESA)
                .user(user)
                .build();
    }
}
