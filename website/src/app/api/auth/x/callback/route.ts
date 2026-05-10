import { NextRequest, NextResponse } from "next/server";
import { clearOAuthCookie, getOAuthCookie, setSession } from "@/lib/server/session";
import { exchangeCode, followsSolWear, getMe } from "@/lib/server/x";

export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  const url = new URL(request.url);
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const oauth = getOAuthCookie(request);

  if (!code || !state || !oauth || oauth.state !== state) {
    return NextResponse.redirect(new URL("/pinboard?auth=failed", request.url));
  }

  try {
    const accessToken = await exchangeCode(code, oauth.verifier);
    const me = await getMe(accessToken);
    const follows = await followsSolWear(accessToken, me.id);
    const response = NextResponse.redirect(new URL(`/pinboard?auth=${follows ? "ok" : "follow"}`, request.url));
    setSession(response, {
      id: me.id,
      username: me.username,
      followsSolWear: follows,
    });
    clearOAuthCookie(response);
    return response;
  } catch {
    const response = NextResponse.redirect(new URL("/pinboard?auth=failed", request.url));
    clearOAuthCookie(response);
    return response;
  }
}
