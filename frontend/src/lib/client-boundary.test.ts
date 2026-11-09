// @vitest-environment node
import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";

import { expect, test } from "vitest";

const SRC_DIR = path.resolve(import.meta.dirname, "..");
const USE_CLIENT = /^\s*["']use client["']/m;
const FORBIDDEN_IN_CLIENT = /["'][^"']*\/auth["']|accessToken|getAccessToken/;

function sourceFiles(): string[] {
  return readdirSync(SRC_DIR, { recursive: true, encoding: "utf8" })
    .filter((file) => /\.tsx?$/.test(file) && !/\.test\.tsx?$/.test(file))
    .map((file) => path.join(SRC_DIR, file));
}

test("RN-40: ningún componente cliente importa lib/auth ni usa el access token", () => {
  const violations = sourceFiles().filter((file) => {
    const content = readFileSync(file, "utf8");
    return USE_CLIENT.test(content) && FORBIDDEN_IN_CLIENT.test(content);
  });

  expect(violations).toEqual([]);
});
