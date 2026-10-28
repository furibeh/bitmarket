import { expect, test } from "vitest";

test("prueba de CI: falla a propósito para comprobar que ci queda en rojo (#3)", () => {
  expect(false).toBe(true);
});
