import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";

import { signIn, signOut } from "./auth-actions";

const mocks = vi.hoisted(() => ({
  signInSocial: vi.fn(),
  signOutApi: vi.fn(),
  redirect: vi.fn(),
  requestHeaders: new Headers({ cookie: "better-auth.session_token=abc" }),
}));

vi.mock("@/lib/auth", () => ({
  auth: {
    api: { signInSocial: mocks.signInSocial, signOut: mocks.signOutApi },
  },
}));
vi.mock("next/headers", () => ({
  headers: async () => mocks.requestHeaders,
}));
vi.mock("next/navigation", () => ({ redirect: mocks.redirect }));

const AUTHORIZE_URL =
  "https://bitmarket.auth.us-east-2.amazoncognito.com/oauth2/authorize?client_id=client-123";

/** Like Next's redirect(), the mock throws so nothing runs after it. */
async function runUntilRedirect(action: () => Promise<void>): Promise<void> {
  await action().catch((error: Error) => {
    if (!error.message.startsWith("NEXT_REDIRECT")) throw error;
  });
}

beforeEach(() => {
  vi.resetAllMocks();
  mocks.redirect.mockImplementation((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  });
  mocks.signInSocial.mockResolvedValue({ url: AUTHORIZE_URL, redirect: true });
  mocks.signOutApi.mockResolvedValue({ success: true });
  vi.stubEnv(
    "COGNITO_DOMAIN",
    "https://bitmarket.auth.us-east-2.amazoncognito.com",
  );
  vi.stubEnv("COGNITO_CLIENT_ID", "client-123");
  vi.stubEnv("BETTER_AUTH_URL", "http://localhost:3000");
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("iniciar sesión", () => {
  test("RN-43: pide a Better Auth el proveedor cognito", async () => {
    await runUntilRedirect(signIn);

    expect(mocks.signInSocial).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ provider: "cognito" }),
      }),
    );
  });

  test("RN-43: redirige a la URL de Managed Login que devuelve Better Auth", async () => {
    await runUntilRedirect(signIn);

    expect(mocks.redirect).toHaveBeenCalledWith(AUTHORIZE_URL);
  });
});

describe("cerrar sesión", () => {
  test("invalida la sesión de Better Auth con las cabeceras de la petición", async () => {
    await runUntilRedirect(signOut);

    expect(mocks.signOutApi).toHaveBeenCalledWith(
      expect.objectContaining({ headers: mocks.requestHeaders }),
    );
  });

  test("redirige al /logout de Cognito con client_id y logout_uri", async () => {
    await runUntilRedirect(signOut);

    expect(mocks.redirect).toHaveBeenCalledTimes(1);
    const url = new URL(mocks.redirect.mock.calls[0][0] as string);
    expect(url.origin).toBe(
      "https://bitmarket.auth.us-east-2.amazoncognito.com",
    );
    expect(url.pathname).toBe("/logout");
    expect(url.searchParams.get("client_id")).toBe("client-123");
    expect(url.searchParams.get("logout_uri")).toBe("http://localhost:3000/");
  });

  test("invalida la sesión antes de redirigir a Cognito", async () => {
    await runUntilRedirect(signOut);

    expect(mocks.signOutApi).toHaveBeenCalled();
    expect(mocks.redirect).toHaveBeenCalled();
    expect(mocks.signOutApi.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.redirect.mock.invocationCallOrder[0],
    );
  });

  test("si Better Auth lanza un error, lo propaga y no redirige", async () => {
    mocks.signOutApi.mockRejectedValue(new Error("auth not initialized"));

    await expect(signOut()).rejects.toThrow("auth not initialized");
    expect(mocks.redirect).not.toHaveBeenCalled();
  });
});
