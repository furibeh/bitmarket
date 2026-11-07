import assert from "node:assert/strict";
import { test } from "node:test";

import { createHandler } from "../src/handler.mjs";

const EMAIL = "comprador@example.com";
const CONSOLE_METHODS = ["debug", "error", "info", "log", "warn"];

function buildEvent(triggerSource) {
  return {
    version: "1",
    region: "us-east-1",
    userPoolId: "us-east-1_TEST",
    userName: "7f3c1a52-0000-4000-8000-000000000001",
    triggerSource,
    callerContext: { awsSdkVersion: "test", clientId: "test-client" },
    request: {
      userAttributes: {
        sub: "7f3c1a52-0000-4000-8000-000000000001",
        email: EMAIL,
        email_verified: "true",
      },
    },
    response: {},
  };
}

function recordingCognito() {
  const calls = [];
  return {
    calls,
    addUserToGroup: async (params) => {
      calls.push(params);
    },
  };
}

function captureConsole(t) {
  const mocks = CONSOLE_METHODS.map((method) =>
    t.mock.method(console, method, () => {}),
  );
  return () =>
    mocks
      .flatMap((mock) => mock.mock.calls)
      .flatMap((call) => call.arguments)
      .map((argument) =>
        argument instanceof Error
          ? `${argument.message} ${argument.stack}`
          : JSON.stringify(argument),
      )
      .join("\n");
}

test("RN-44: añade al grupo CUSTOMER al confirmar el registro", async () => {
  const cognito = recordingCognito();
  const event = buildEvent("PostConfirmation_ConfirmSignUp");

  const result = await createHandler(cognito)(event);

  assert.deepEqual(cognito.calls, [
    {
      userPoolId: "us-east-1_TEST",
      username: "7f3c1a52-0000-4000-8000-000000000001",
      groupName: "CUSTOMER",
    },
  ]);
  assert.deepEqual(result, event);
});

test("RN-44: no toca los grupos al recuperar la contraseña", async () => {
  const cognito = recordingCognito();
  const event = buildEvent("PostConfirmation_ConfirmForgotPassword");

  const result = await createHandler(cognito)(event);

  assert.deepEqual(cognito.calls, []);
  assert.deepEqual(result, event);
});

test("RN-44: ignora un disparador desconocido", async () => {
  const cognito = recordingCognito();
  const event = buildEvent("PostConfirmation_SomethingNew");

  const result = await createHandler(cognito)(event);

  assert.deepEqual(cognito.calls, []);
  assert.deepEqual(result, event);
});

test("ADR-0013: propaga el error si Cognito falla", async () => {
  const failure = new Error("cognito unavailable");
  const handler = createHandler({
    addUserToGroup: async () => {
      throw failure;
    },
  });

  await assert.rejects(handler(buildEvent("PostConfirmation_ConfirmSignUp")), failure);
});

test("Registro sin datos personales: no escribe el correo en los logs al asignar el grupo", async (t) => {
  const logged = captureConsole(t);
  const handler = createHandler(recordingCognito());

  await handler(buildEvent("PostConfirmation_ConfirmSignUp"));

  assert.ok(!logged().includes(EMAIL), "the email reached the logs");
});

test("Registro sin datos personales: no escribe el correo en los logs si Cognito falla", async (t) => {
  const logged = captureConsole(t);
  const handler = createHandler({
    addUserToGroup: async () => {
      throw new Error("cognito unavailable");
    },
  });

  await handler(buildEvent("PostConfirmation_ConfirmSignUp")).catch(() => {});

  assert.ok(!logged().includes(EMAIL), "the email reached the logs");
});
