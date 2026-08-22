import fc from "fast-check";
import { describe, expect, it } from "vitest";
import { formatMoney } from "./format";

const TWO_DECIMALS = /^-?\d+\.\d{2}$/;

describe("formatMoney", () => {
  it("keeps values that already carry two decimals", () => {
    expect(formatMoney("1234.50")).toBe("1234.50");
    expect(formatMoney(1234.5)).toBe("1234.50");
  });

  it("pads values with fewer than two decimals", () => {
    expect(formatMoney("0")).toBe("0.00");
    expect(formatMoney("7.5")).toBe("7.50");
    expect(formatMoney(42)).toBe("42.00");
  });

  it("scales longer fractions half-up without floating point drift", () => {
    expect(formatMoney("0.005")).toBe("0.01");
    expect(formatMoney("1.004")).toBe("1.00");
    expect(formatMoney("1.995")).toBe("2.00");
    expect(formatMoney("999999999999.995")).toBe("1000000000000.00");
  });

  it("renders negative amounts with a leading sign", () => {
    expect(formatMoney("-12.3")).toBe("-12.30");
    expect(formatMoney(-0.001)).toBe("0.00");
  });

  it("renders absent or unusable values as zero", () => {
    expect(formatMoney(null)).toBe("0.00");
    expect(formatMoney(undefined)).toBe("0.00");
    expect(formatMoney("")).toBe("0.00");
    expect(formatMoney("not a number")).toBe("0.00");
    expect(formatMoney(Number.NaN)).toBe("0.00");
  });

  it("always renders exactly two decimal places", () => {
    const moneyInput = fc.oneof(
      fc.constant(null),
      fc.constant(undefined),
      fc.double({ noDefaultInfinity: true, noNaN: true, min: -1e12, max: 1e12 }),
      fc.integer({ min: -1_000_000, max: 1_000_000 }),
      fc
        .tuple(
          fc.constantFrom("", "-", "+"),
          fc.stringMatching(/^\d{1,15}$/),
          fc.stringMatching(/^\d{0,6}$/),
        )
        .map(([sign, whole, fraction]) =>
          fraction.length > 0 ? `${sign}${whole}.${fraction}` : `${sign}${whole}`,
        ),
    );

    fc.assert(
      fc.property(moneyInput, (value) => {
        expect(formatMoney(value)).toMatch(TWO_DECIMALS);
      }),
    );
  });
});
