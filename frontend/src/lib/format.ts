
export type MoneyInput = string | number | null | undefined;

const ZERO = "0.00";
const DECIMAL_PATTERN = /^([+-]?)(\d*)(?:\.(\d*))?$/;

export function formatMoney(value: MoneyInput): string {
  if (value === null || value === undefined) {
    return ZERO;
  }

  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      return ZERO;
    }
    const asText = value.toString();
    return DECIMAL_PATTERN.test(asText) ? formatDecimalText(asText) : value.toFixed(2);
  }

  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return ZERO;
  }
  if (DECIMAL_PATTERN.test(trimmed)) {
    return formatDecimalText(trimmed);
  }

  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? formatMoney(parsed) : ZERO;
}

function formatDecimalText(text: string): string {
  const match = DECIMAL_PATTERN.exec(text);
  if (!match) {
    return ZERO;
  }

  const [, sign, wholeDigits, fractionDigits = ""] = match;
  const whole = wholeDigits.length > 0 ? wholeDigits : "0";
  const fraction = `${fractionDigits}00`.slice(0, 2);

  let cents = BigInt(`${whole}${fraction}`);
  // Half-up on the third decimal digit, mirroring the backend rounding mode.
  if (fractionDigits.length > 2 && fractionDigits[2] >= "5") {
    cents += BigInt(1);
  }

  const digits = cents.toString().padStart(3, "0");
  const negative = sign === "-" && cents !== BigInt(0);
  return `${negative ? "-" : ""}${digits.slice(0, -2)}.${digits.slice(-2)}`;
}
