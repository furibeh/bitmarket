import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";

import { SiteHeader } from "./site-header";

const mocks = vi.hoisted(() => ({ getSession: vi.fn() }));

vi.mock("@/lib/auth", () => ({
  auth: { api: { getSession: mocks.getSession } },
}));
vi.mock("@/lib/auth-actions", () => ({ signIn: vi.fn(), signOut: vi.fn() }));
vi.mock("next/headers", () => ({ headers: async () => new Headers() }));

const EMAIL = "comprador@example.com";

function signInButton() {
  return screen.queryByRole("button", { name: "Iniciar sesión" });
}

function signOutButton() {
  return screen.queryByRole("button", { name: "Cerrar sesión" });
}

beforeEach(() => {
  mocks.getSession.mockReset();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("cabecera de la tienda", () => {
  test("con sesión muestra el correo del comprador", async () => {
    mocks.getSession.mockResolvedValue({ user: { email: EMAIL } });

    render(await SiteHeader());

    expect(screen.queryByText(EMAIL)).not.toBeNull();
  });

  test("con sesión ofrece cerrar sesión y no iniciarla", async () => {
    mocks.getSession.mockResolvedValue({ user: { email: EMAIL } });

    render(await SiteHeader());

    expect(signOutButton()).not.toBeNull();
    expect(signInButton()).toBeNull();
  });

  test("sin sesión ofrece iniciar sesión y no cerrarla", async () => {
    mocks.getSession.mockResolvedValue(null);

    render(await SiteHeader());

    expect(signInButton()).not.toBeNull();
    expect(signOutButton()).toBeNull();
  });

  test("si falla la consulta de sesión ofrece iniciar sesión", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    mocks.getSession.mockRejectedValue(new Error("connection refused"));

    render(await SiteHeader());

    expect(signInButton()).not.toBeNull();
  });

  test("si falla la consulta de sesión registra el fallo sin incluir el correo", async () => {
    const logged = vi.spyOn(console, "error").mockImplementation(() => {});
    mocks.getSession.mockRejectedValue(new Error(`failed for ${EMAIL}`));

    render(await SiteHeader());

    expect(logged).toHaveBeenCalled();
    expect(JSON.stringify(logged.mock.calls.map(String))).not.toContain(EMAIL);
  });

  test("si falla la consulta de sesión registra en JSON el tipo de fallo", async () => {
    const logged = vi.spyOn(console, "error").mockImplementation(() => {});
    mocks.getSession.mockRejectedValue(
      Object.assign(new TypeError("connection refused"), {
        code: "ECONNREFUSED",
      }),
    );

    render(await SiteHeader());

    expect(JSON.parse(String(logged.mock.calls[0][0]))).toMatchObject({
      level: "error",
      errorName: "TypeError",
      errorCode: "ECONNREFUSED",
    });
  });
});
