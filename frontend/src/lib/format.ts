/**
 * Money rendering helpers.
 *
 * The API sends every monetary field already scaled to two decimals (Requirement 32.7),
 * either as a JSON number or as a plain string. The frontend renders those values and
 * never recomputes them, so `formatMoney` only adjusts presentation: it pads or trims the
 * fractional part to exactly two digits. Scaling is done on the decimal digits themselves
 * (via BigInt cents) so no binary floating point error is introduced.
 */

export type MoneyInput = string | number | null | undefined;

const ZERO = "0.00";
const DECIMAL_PATTERN = /^([+-]?)(\d*)(?:\.(\d*))?$/;

/**
 * Renders a money value with exactly two decimal places.
 *
 * Total by construction: `null`, `undefined`, blank input and unparseable input all render
 * as `0.00`, matching the backend `Money.money(null)` convention.
 */
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
