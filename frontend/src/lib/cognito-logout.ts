export function buildCognitoLogoutUrl({
  domainUrl,
  clientId,
  appUrl,
}: {
  domainUrl: string;
  clientId: string;
  appUrl: string;
}): string {
  const url = new URL("/logout", domainUrl);
  url.searchParams.set("client_id", clientId);
  url.searchParams.set("logout_uri", new URL("/", appUrl).toString());
  return url.toString();
}
