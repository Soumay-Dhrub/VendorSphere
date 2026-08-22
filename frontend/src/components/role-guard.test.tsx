import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RoleGuard } from "./role-guard";

function storeUserWithRoles(roles: string[]) {
  window.localStorage.setItem(
    "user",
    JSON.stringify({
      id: "11111111-1111-1111-1111-111111111111",
      organizationId: "22222222-2222-2222-2222-222222222222",
      departmentId: null,
      email: "user@example.com",
      firstName: "Test",
      lastName: "User",
      phone: null,
      active: true,
      roles,
      lastLoginAt: null,
      createdAt: "2026-01-01T00:00:00Z",
    }),
  );
}

describe("RoleGuard", () => {
  it("renders the guarded content when the user holds an allowed role", () => {
    render(
      <RoleGuard allow={["FINANCE"]} roles={["FINANCE"]}>
        <p>Payment run</p>
      </RoleGuard>,
    );

    expect(screen.getByText("Payment run")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders an access denied message in place of the content when roles do not match", () => {
    render(
      <RoleGuard allow={["ADMIN"]} roles={["REQUESTER"]}>
        <p>Audit log</p>
      </RoleGuard>,
    );

    expect(screen.queryByText("Audit log")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Access denied");
    expect(screen.getByRole("alert")).toHaveTextContent("Required role: ADMIN");
  });

  it("reads the roles of the signed-in user when none are supplied", () => {
    storeUserWithRoles(["PROCUREMENT_OFFICER"]);

    render(
      <RoleGuard allow={["PROCUREMENT_OFFICER", "PROCUREMENT_MANAGER"]}>
        <p>RFQ list</p>
      </RoleGuard>,
    );

    expect(screen.getByText("RFQ list")).toBeInTheDocument();
  });

  it("denies access when no user is signed in", () => {
    render(
      <RoleGuard allow={["ADMIN"]}>
        <p>Audit log</p>
      </RoleGuard>,
    );

    expect(screen.queryByText("Audit log")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Access denied");
  });
});
