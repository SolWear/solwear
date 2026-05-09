import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/server/db";

export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  if (!isAdmin(request)) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const ideas = db()
    .prepare("SELECT id, username, idea, status, created_at FROM pinboard_ideas ORDER BY id DESC LIMIT 200")
    .all();
  return NextResponse.json({ ideas });
}

export async function POST(request: NextRequest) {
  if (!isAdmin(request)) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = (await request.json().catch(() => null)) as { id?: number; status?: string } | null;
  if (!body?.id || !["approved", "rejected", "pending"].includes(body.status || "")) {
    return NextResponse.json({ error: "Invalid moderation request" }, { status: 400 });
  }
  db().prepare("UPDATE pinboard_ideas SET status = ?, moderated_at = ? WHERE id = ?").run(
    body.status,
    new Date().toISOString(),
    body.id,
  );
  return NextResponse.json({ success: true });
}

function isAdmin(request: NextRequest): boolean {
  const token = process.env.PINBOARD_ADMIN_TOKEN;
  return Boolean(token && request.headers.get("x-admin-token") === token);
}
