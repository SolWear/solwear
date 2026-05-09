import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/server/db";
import { rateLimit } from "@/lib/server/rateLimit";

export const runtime = "nodejs";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function POST(request: NextRequest) {
  const ip = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "local";
  if (!rateLimit(`notify:${ip}`, 5, 15 * 60 * 1000)) {
    return NextResponse.json({ error: "Too many attempts" }, { status: 429 });
  }

  const body = (await request.json().catch(() => null)) as { email?: string } | null;
  const email = body?.email?.trim().toLowerCase();
  if (!email || !EMAIL_RE.test(email) || email.length > 254) {
    return NextResponse.json({ error: "Enter a valid email" }, { status: 400 });
  }

  db().prepare("INSERT INTO notify_emails (email, created_at) VALUES (?, ?)").run(
    email,
    new Date().toISOString(),
  );

  return NextResponse.json({ success: true });
}
