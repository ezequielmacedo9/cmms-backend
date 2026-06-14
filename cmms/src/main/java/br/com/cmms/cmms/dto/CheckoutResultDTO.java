package br.com.cmms.cmms.dto;

/**
 * Checkout response. {@code linkPagamento} is empty until a payment gateway
 * (Asaas/Stripe) is wired — the UI handles the empty case gracefully.
 */
public record CheckoutResultDTO(
    AssinaturaInfoDTO assinatura,
    String linkPagamento
) {}
