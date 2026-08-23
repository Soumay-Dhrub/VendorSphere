import { describe, expect, it } from "vitest";
import { NAV_ITEMS, isCurrentNavItem, navItemsForRoles } from "./app-nav";

function labelsFor(roles: string[]): string[] {
  return navItemsForRoles(roles).map((item) => item.label);
}

describe("navItemsForRoles", () => {
  it("shows every area to an administrator", () => {
    expect(labelsFor(["ADMIN"])).toEqual(NAV_ITEMS.map((item) => item.label));
  });

  it("hides finance areas from a procurement officer", () => {
    const labels = labelsFor(["PROCUREMENT_OFFICER"]);

    expect(labels).toContain("RFQs");
    expect(labels).toContain("Purchase orders");
    expect(labels).not.toContain("Invoices");
    expect(labels).not.toContain("Payments");
    expect(labels).not.toContain("Audit logs");
  });

  it("shows finance only the invoice and payment areas beside the shared ones", () => {
    expect(labelsFor(["FINANCE"])).toEqual([
      "Dashboard",
      "Invoices",
      "Payments",
      "Notifications",
    ]);
  });

  it("shows a requester purchase requests but no vendor or finance areas", () => {
    expect(labelsFor(["REQUESTER"])).toEqual([
      "Dashboard",
      "Purchase requests",
      "Notifications",
    ]);
  });

  it("shows a vendor user only notifications in the internal shell", () => {
    expect(labelsFor(["VENDOR"])).toEqual(["Notifications"]);
  });

  it("shows nothing to a user holding no role", () => {
    expect(labelsFor([])).toEqual([]);
  });

  it("keeps declaration order for a user holding several roles", () => {
    const labels = labelsFor(["FINANCE", "PROCUREMENT_OFFICER"]);

    expect(labels).toEqual(NAV_ITEMS.filter((item) => labels.includes(item.label)).map((i) => i.label));
  });
});

describe("isCurrentNavItem", () => {
  it("marks the exact path", () => {
    expect(isCurrentNavItem("/vendors", "/vendors")).toBe(true);
  });

  it("marks the area of a detail route", () => {
    expect(isCurrentNavItem("/vendors", "/vendors/abc-123")).toBe(true);
  });

  it("does not mark an area whose href is only a string prefix", () => {
    expect(isCurrentNavItem("/purchase-orders", "/purchase-orders-archive")).toBe(false);
  });

  it("marks nothing when the path is unknown", () => {
    expect(isCurrentNavItem("/vendors", null)).toBe(false);
  });
});
