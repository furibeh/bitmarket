import { describe, expect, test } from "vitest";

import { buildCognitoLogoutUrl } from "./cognito-logout";

const params = {
  domainUrl: "https://bitmarket.auth.us-east-2.amazoncognito.com",
  clientId: "client-123",
  appUrl: "http://localhost:3000",
};

describe("URL de cierre de sesión de Cognito", () => {
  test("apunta al endpoint /logout del dominio de Cognito", () => {
    const url = new URL(buildCognitoLogoutUrl(params));

    expect(url.origin).toBe(params.domainUrl);
    expect(url.pathname).toBe("/logout");
  });

  test("incluye el client_id del app client", () => {
    const url = new URL(buildCognitoLogoutUrl(params));

    expect(url.searchParams.get("client_id")).toBe("client-123");
  });

  test("usa como logout_uri exactamente la URL registrada en el app client", () => {
    const url = new URL(buildCognitoLogoutUrl(params));

    expect(url.searchParams.get("logout_uri")).toBe("http://localhost:3000/");
  });

  test("codifica logout_uri como parámetro de consulta", () => {
    expect(buildCognitoLogoutUrl(params)).toContain(
      "logout_uri=http%3A%2F%2Flocalhost%3A3000%2F",
    );
  });

  test("no duplica la barra si la URL de la tienda ya termina en barra", () => {
    const url = new URL(
      buildCognitoLogoutUrl({ ...params, appUrl: "http://localhost:3000/" }),
    );

    expect(url.searchParams.get("logout_uri")).toBe("http://localhost:3000/");
  });

  test("no duplica la barra si el dominio de Cognito termina en barra", () => {
    const url = new URL(
      buildCognitoLogoutUrl({
        ...params,
        domainUrl: `${params.domainUrl}/`,
      }),
    );

    expect(url.pathname).toBe("/logout");
  });
});
