import { NextResponse } from "next/server";
import { setOAuthCookie } from "@/lib/server/session";
import { randomToken, xAuthorizeUrl } from "@/lib/server/x";

export const runtime = "nodejs";

export async function GET() {
  try {
    const state = randomToken();
    const verifier = randomToken();
    const response = NextResponse.redirect(xAuthorizeUrl(state, verifier));
    setOAuthCookie(response, { state, verifier, createdAt: Date.now() });
    return response;
  } catch (error) {
    const message = error instanceof Error ? error.message : "X auth is not configured";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
