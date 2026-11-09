import { headers } from "next/headers";

import { auth } from "@/lib/auth";
import { signIn, signOut } from "@/lib/auth-actions";

async function currentUserEmail(): Promise<string | null> {
  // Outside the try: Next.js signals dynamic rendering by throwing from headers().
  const requestHeaders = await headers();

  try {
    const session = await auth.api.getSession({ headers: requestHeaders });
    return session?.user.email ?? null;
  } catch (error) {
    // A broken session store must not take the public catalog down. Only the
    // error type is logged because the message may repeat the customer's email.
    const { name, code } = Object(error) as { name?: string; code?: unknown };
    console.error(
      JSON.stringify({
        level: "error",
        message: "Failed to load the session",
        errorName: name,
        errorCode: code,
      }),
    );
    return null;
  }
}

export async function SiteHeader() {
  const email = await currentUserEmail();

  return (
    <header>
      {email ? (
        <form action={signOut}>
          <span>{email}</span>
          <button type="submit">Cerrar sesión</button>
        </form>
      ) : (
        <form action={signIn}>
          <button type="submit">Iniciar sesión</button>
        </form>
      )}
    </header>
  );
}
