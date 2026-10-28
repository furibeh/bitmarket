import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "BitMarket",
  description: "Tienda en línea de tecnología",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
