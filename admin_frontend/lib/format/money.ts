const RUB_FORMATTER = new Intl.NumberFormat('ru-RU', {
  style: 'currency',
  currency: 'RUB',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatRublesFromKopecks(amountKopecks: number): string {
  if (!Number.isFinite(amountKopecks)) return RUB_FORMATTER.format(0);
  return RUB_FORMATTER.format(amountKopecks / 100);
}

export function formatOptionalRublesFromKopecks(amountKopecks: number): string {
  return amountKopecks > 0 ? formatRublesFromKopecks(amountKopecks) : '—';
}

export function kopecksToRublesInput(amountKopecks: number): string {
  if (!Number.isFinite(amountKopecks)) return '0.00';
  return (amountKopecks / 100).toFixed(2);
}

export function rublesInputToKopecks(value: string): number {
  const normalized = value.replace(',', '.').trim();
  const rubles = Number(normalized);
  if (!Number.isFinite(rubles)) return NaN;
  return Math.round(rubles * 100);
}
