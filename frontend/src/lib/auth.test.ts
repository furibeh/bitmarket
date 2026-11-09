// @vitest-environment node
import { createHmac } from "node:crypto";

import { betterAuth } from "better-auth";
import { memoryAdapter } from "better-auth/adapters/memory";
import { beforeEach, describe, expect, test, vi } from "vitest";

import { createAuthOptions } from "./auth";

const env = vi.hoisted(() => {
  const values = {
    BETTER_AUTH_SECRET: "test-secret-with-at-least-32-characters",
    BETTER_AUTH_URL: "http://localhost:3000",
    BFF_DATABASE_URL: "postgres://bitmarket_bff:unused@localhost:5432/unused",
    COGNITO_CLIENT_ID: "test-client-id",
    COGNITO_CLIENT_SECRET: "test-client-secret",
    COGNITO_DOMAIN: "https://bitmarket.auth.us-east-2.amazoncognito.com",
    COGNITO_REGION: "us-east-2",
    COGNITO_USER_POOL_ID: "us-east-2_test",
  };
  Object.assign(process.env, values);
  return values;
});

const ORIGIN = env.BETTER_AUTH_URL;
const USER_ID = "user-1";
const ACCOUNT_ID = "account-1";
const SESSION_TOKEN = "session-token-1";
const EMAIL = "comprador@example.com";
const ACCESS_TOKEN = "cognito-access-token-value";
const REFRESH_TOKEN = "cognito-refresh-token-value";
const ID_TOKEN = "cognito-id-token-value";

function seededDatabase() {
  const now = new Date();
  const inOneHour = new Date(now.getTime() + 60 * 60 * 1000);
  return {
    user: [
      {
        id: USER_ID,
        name: "",
        email: EMAIL,
        emailVerified: true,
        createdAt: now,
        updatedAt: now,
      },
    ],
    session: [
      {
        id: "session-1",
        userId: USER_ID,
        token: SESSION_TOKEN,
        expiresAt: inOneHour,
        createdAt: now,
        updatedAt: now,
      },
    ],
    account: [
      {
        id: ACCOUNT_ID,
        userId: USER_ID,
        providerId: "cognito",
        accountId: "cognito-sub-1",
        accessToken: ACCESS_TOKEN,
        refreshToken: REFRESH_TOKEN,
        idToken: ID_TOKEN,
        accessTokenExpiresAt: inOneHour,
        createdAt: now,
        updatedAt: now,
      },
    ],
    verification: [],
  };
}

function sessionCookie(): string {
  const signature = createHmac("sha256", env.BETTER_AUTH_SECRET)
    .update(SESSION_TOKEN)
    .digest("base64");
  const value = encodeURIComponent(`${SESSION_TOKEN}.${signature}`);
  return `better-auth.session_token=${value}`;
}

function browserRequest(path: string, init: RequestInit = {}): Request {
  return new Request(`${ORIGIN}/api/auth${path}`, {
    ...init,
    headers: {
      "content-type": "application/json",
      origin: ORIGIN,
      cookie: sessionCookie(),
    },
  });
}

describe("exposición de los tokens de Cognito", () => {
  let auth: ReturnType<typeof betterAuth>;

  beforeEach(() => {
    auth = betterAuth(createAuthOptions(memoryAdapter(seededDatabase())));
  });

  test.each(["/get-access-token", "/refresh-token"])(
    "RN-40: POST %s responde 404 al navegador aunque tenga sesión",
    async (path) => {
      const response = await auth.handler(
        browserRequest(path, {
          method: "POST",
          body: JSON.stringify({ accountId: ACCOUNT_ID }),
        }),
      );

      expect(response.status).toBe(404);
    },
  );

  test("RN-40: el servidor obtiene el access token con auth.api.getAccessToken", async () => {
    await expect(
      auth.api.getAccessToken({
        body: { accountId: ACCOUNT_ID, userId: USER_ID },
      }),
    ).resolves.toMatchObject({ accessToken: ACCESS_TOKEN });
  });

  test("RN-40: la respuesta de get-session identifica al usuario sin contener tokens de Cognito", async () => {
    const response = await auth.handler(browserRequest("/get-session"));
    const exposed = `${await response.text()} ${response.headers.get("set-cookie") ?? ""}`;

    expect(exposed).toContain(EMAIL);
    expect(exposed).not.toContain(ACCESS_TOKEN);
    expect(exposed).not.toContain(REFRESH_TOKEN);
    expect(exposed).not.toContain(ID_TOKEN);
  });
});
