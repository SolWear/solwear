"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Nav from "@/components/Nav";
import Footer from "@/components/Footer";

type Idea = {
  id: number;
  username: string;
  idea: string;
  created_at: string;
};

type User = {
  id: string;
  username: string;
  followsSolWear: boolean;
};

type Quota = {
  submitted: number;
  remaining: number;
};

type PinboardResponse = {
  user: User | null;
  ideas: Idea[];
  quota?: Quota;
  csrfToken?: string | null;
  maxIdeasPerAccount?: number;
};

const dynamicEnabled = process.env.NEXT_PUBLIC_SOLWEAR_DYNAMIC === "1";

export default function PinboardPage() {
  const [ideas, setIdeas] = useState<Idea[]>([]);
  const [user, setUser] = useState<User | null>(null);
  const [quota, setQuota] = useState<Quota>({ submitted: 0, remaining: 0 });
  const [maxIdeas, setMaxIdeas] = useState(3);
  const [csrfToken, setCsrfToken] = useState<string | null>(null);
  const [idea, setIdea] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [composerOpen, setComposerOpen] = useState(false);

  const boardIdeas = ideas.length ? ideas : previewIdeas;
  const canPost = dynamicEnabled && !!user && user.followsSolWear && quota.remaining > 0;
  const boardTitle = useMemo(() => {
    if (!dynamicEnabled) return "Preview board";
    if (!user) return "Sign in to pin";
    if (!user.followsSolWear) return "Follow SolWear first";
    if (quota.remaining <= 0) return "Idea limit reached";
    return `${quota.remaining}/${maxIdeas} pins left`;
  }, [maxIdeas, quota.remaining, user]);

  useEffect(() => {
    if (!dynamicEnabled) return;
    refreshBoard();
  }, []);

  async function refreshBoard() {
    try {
      const res = await fetch("/api/pinboard/");
      const data = (await res.json()) as PinboardResponse;
      setIdeas(data.ideas || []);
      setUser(data.user || null);
      setQuota(data.quota || { submitted: 0, remaining: 0 });
      setCsrfToken(data.csrfToken || null);
      setMaxIdeas(data.maxIdeasPerAccount || 3);
    } catch {
      setStatus("Pinboard API is offline.");
    }
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!dynamicEnabled) {
      setStatus("The live pinboard opens on the Docker/VPS deployment.");
      return;
    }
    if (!csrfToken) {
      setStatus("Refresh the page before posting.");
      return;
    }
    setPending(true);
    setStatus(null);
    try {
      const res = await fetch("/api/pinboard/", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-csrf-token": csrfToken,
        },
        body: JSON.stringify({ idea }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Could not submit idea");
      setIdea("");
      setQuota(data.quota || quota);
      setStatus(data.status === "approved" ? "Pinned." : "Submitted for review.");
      setComposerOpen(false);
      await refreshBoard();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Could not submit idea");
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <Nav />
      <main className="min-h-screen px-6 pb-24 pt-28">
        <section className="mx-auto max-w-7xl">
          <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
            <div>
              <p className="label-caps mb-3">community</p>
              <h1 className="text-4xl font-bold md:text-6xl">SolWear pinboard</h1>
              <p className="mt-5 max-w-2xl text-sm leading-7 text-white/58">
                A live board for follower ideas. Sign in with X, follow @SolWear_, then pin up to
                three ideas from your account.
              </p>
            </div>
            <div className="text-sm text-white/58 md:text-right">
              <p className="font-semibold text-white">{boardTitle}</p>
              {user && <p className="mt-1">@{user.username}</p>}
            </div>
          </div>

          <div className="mt-10 min-h-[520px] border border-white/10 bg-[linear-gradient(90deg,rgba(255,255,255,0.04)_1px,transparent_1px),linear-gradient(rgba(255,255,255,0.04)_1px,transparent_1px)] bg-[size:32px_32px] p-4 md:p-8">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {boardIdeas.map((item, index) => (
                <article
                  key={item.id}
                  className="min-h-40 border border-white/10 bg-white p-5 text-black shadow-[0_18px_40px_rgba(0,0,0,0.28)]"
                  style={{ transform: `rotate(${[-1.2, 0.8, -0.4, 1.1, -0.8][index % 5]}deg)` }}
                >
                  <div className="mb-4 h-2 w-2 rounded-full bg-solwear-red shadow-[0_0_0_5px_rgba(224,0,15,0.12)]" />
                  <p className="text-sm leading-6">{item.idea}</p>
                  <p className="mt-5 text-xs text-black/45">@{item.username}</p>
                </article>
              ))}
            </div>
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            {dynamicEnabled ? (
              user ? (
                <>
                  {!user.followsSolWear && (
                    <a className="focus-ring inline-flex min-h-11 items-center bg-white px-5 py-3 text-sm font-semibold text-black" href="https://x.com/SolWear_" target="_blank" rel="noreferrer">
                      Follow @SolWear_
                    </a>
                  )}
                  <form action="/api/auth/logout/" method="post">
                    <button className="focus-ring min-h-11 border border-white/20 px-5 py-3 text-sm text-white/70" type="submit">
                      Sign out
                    </button>
                  </form>
                </>
              ) : (
                <a className="focus-ring inline-flex min-h-11 items-center bg-white px-5 py-3 text-sm font-semibold text-black" href="/api/auth/x/start/">
                  Sign in with X
                </a>
              )
            ) : (
              <a className="focus-ring inline-flex min-h-11 items-center border border-white/20 px-5 py-3 text-sm text-white/80" href="https://x.com/SolWear_" target="_blank" rel="noreferrer">
                Follow @SolWear_
              </a>
            )}
            {status && <p className="text-sm text-white/60">{status}</p>}
          </div>
        </section>

        <button
          type="button"
          onClick={() => {
            setStatus(null);
            setComposerOpen(true);
          }}
          disabled={dynamicEnabled && !canPost}
          className="focus-ring fixed bottom-6 right-6 flex h-14 w-14 items-center justify-center rounded-full bg-white text-3xl leading-none text-black shadow-[0_18px_40px_rgba(0,0,0,0.35)] disabled:cursor-not-allowed disabled:opacity-45"
          aria-label="Add idea"
          title={dynamicEnabled && !canPost ? boardTitle : "Add idea"}
        >
          +
        </button>

        {composerOpen && (
          <div className="fixed inset-0 z-[60] flex items-end bg-black/72 px-4 py-5 backdrop-blur-sm md:items-center md:justify-center">
            <form onSubmit={submit} className="w-full max-w-lg border border-white/10 bg-[#0b0b0b] p-5 shadow-2xl">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="label-caps mb-1">new pin</p>
                  <h2 className="text-2xl font-semibold">Add an idea</h2>
                </div>
                <button type="button" onClick={() => setComposerOpen(false)} className="focus-ring min-h-10 px-3 text-sm text-white/60">
                  Close
                </button>
              </div>

              <textarea
                value={idea}
                onChange={(e) => setIdea(e.target.value)}
                minLength={6}
                maxLength={240}
                required
                className="mt-5 min-h-36 w-full border border-white/15 bg-black p-3 text-sm text-white outline-none focus:border-white/45"
                placeholder="A wallet clockface that..."
              />
              <div className="mt-2 flex items-center justify-between text-xs text-white/40">
                <span>{quota.remaining}/{maxIdeas} pins left</span>
                <span>{idea.length}/240</span>
              </div>
              <button
                type="submit"
                disabled={pending || !canPost}
                className="focus-ring mt-5 min-h-11 w-full bg-white px-5 py-3 text-sm font-semibold text-black disabled:cursor-not-allowed disabled:opacity-50"
              >
                {pending ? "Submitting..." : "Pin idea"}
              </button>
            </form>
          </div>
        )}
      </main>
      <Footer />
    </>
  );
}

const previewIdeas: Idea[] = [
  {
    id: 1,
    username: "SolWear_",
    idea: "Make the NFC sync effect feel like a tiny unlock ritual across phone and watch.",
    created_at: new Date().toISOString(),
  },
  {
    id: 2,
    username: "SolWear_",
    idea: "Add a wallet clockface that shows balance, NFC armed state, and last sync.",
    created_at: new Date().toISOString(),
  },
  {
    id: 3,
    username: "SolWear_",
    idea: "Let the watch show a tiny receipt card after every successful NFC wallet sync.",
    created_at: new Date().toISOString(),
  },
];
