import { render, screen } from "@testing-library/react";
import { expect, test } from "vitest";

import Home from "./page";

test("muestra el nombre BitMarket en la página de inicio", () => {
  render(<Home />);

  expect(
    screen.getByRole("heading", { level: 1, name: "BitMarket" }),
  ).toBeDefined();
});
