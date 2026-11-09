import { betterAuth, type BetterAuthOptions } from "better-auth";
import { nextCookies } from "better-auth/next-js";
import { Pool } from "pg";

// BETTER_AUTH_SECRET and BETTER_AUTH_URL are read by Better Auth itself.
export function createAuthOptions(
  database: BetterAuthOptions["database"],
): BetterAuthOptions {
  return {
    database,
    // These routes return Cognito tokens over HTTP. Disabling them only affects the
    // router: the server still calls auth.api.getAccessToken directly (RN-40, ADR-0011).
    disabledPaths: ["/get-access-token", "/refresh-token"],
    socialProviders: {
      cognito: {
        clientId: process.env.COGNITO_CLIENT_ID!,
        clientSecret: process.env.COGNITO_CLIENT_SECRET!,
        domain: process.env.COGNITO_DOMAIN!,
        region: process.env.COGNITO_REGION!,
        userPoolId: process.env.COGNITO_USER_POOL_ID!,
      },
    },
    plugins: [nextCookies()],
  };
}

export const auth = betterAuth(
  createAuthOptions(
    new Pool({ connectionString: process.env.BFF_DATABASE_URL }),
  ),
);
