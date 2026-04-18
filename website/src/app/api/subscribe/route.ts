import { NextRequest, NextResponse } from "next/server";
import Database from "better-sqlite3";
import fs from "node:fs";
import path from "node:path";

export const runtime = "nodejs";

const DB_DIR = process.env.SOLWEAR_DATA_DIR || "/data";
const DB_PATH = path.join(DB_DIR, "emails.db");

let db: Database.Database | null = null;

function getDb() {
  if (db) return db;
  fs.mkdirSync(DB_DIR, { recursive: true });
  db = new Database(DB_PATH);
  db.pragma("journal_mode = WAL");
  db.exec(`
    CREATE TABLE IF NOT EXISTS subscribers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT NOT NULL UNIQUE,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      source TEXT,
      ip TEXT
    );
  `);
  return db;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function POST(req: NextRequest) {
  try {
    const body = await req.json().catch(() => null);
    const email = String(body?.email || "").trim().toLowerCase();

    if (!email || !EMAIL_RE.test(email) || email.length > 254) {
      return NextResponse.json({ ok: false, error: "Invalid email address." }, { status: 400 });
    }

    const ip =
      req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
      req.headers.get("x-real-ip") ||
      null;

    const stmt = getDb().prepare(
      "INSERT OR IGNORE INTO subscribers (email, source, ip) VALUES (?, ?, ?)"
    );
    const result = stmt.run(email, "preorder", ip);

    return NextResponse.json({ ok: true, inserted: result.changes > 0 });
  } catch (err) {
    console.error("subscribe error", err);
    return NextResponse.json({ ok: false, error: "Server error." }, { status: 500 });
  }
}

export async function GET() {
  try {
    const row = getDb().prepare("SELECT COUNT(*) AS c FROM subscribers").get() as { c: number };
    return NextResponse.json({ ok: true, count: row.c });
  } catch {
    return NextResponse.json({ ok: false, count: 0 });
  }
}
