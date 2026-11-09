"use server";

import { headers } from "next/headers";
import { redirect } from "next/navigation";

import { auth } from "@/lib/auth";
import { buildCognitoLogoutUrl } from "@/lib/cognito-logout";

export async function signIn(): Promise<void> {
  const { url } = await auth.api.signInSocial({
    body: { provider: "cognito", callbackURL: "/" },
    headers: await headers(),
  });

  redirect(url!);
}

export async function signOut(): Promise<void> {
  await auth.api.signOut({ headers: await headers() });

  // Without this redirect the Managed Login session survives and the next
  // login skips the credentials prompt.
  redirect(
    buildCognitoLogoutUrl({
      domainUrl: process.env.COGNITO_DOMAIN!,
      clientId: process.env.COGNITO_CLIENT_ID!,
      appUrl: process.env.BETTER_AUTH_URL!,
    }),
  );
}
