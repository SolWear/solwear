import Nav from "@/components/Nav";
import Footer from "@/components/Footer";

const people = [
  "Everyone testing NFC taps on real Android phones",
  "Frontier Hackathon reviewers and builders",
  "The Solana builders who keep hardware weird and alive",
  "Friends giving design, firmware, and demo feedback",
];

export default function ThanksPage() {
  return (
    <>
      <Nav />
      <main className="min-h-screen px-6 pb-24 pt-28">
        <section className="mx-auto max-w-5xl">
          <p className="label-caps mb-3">thanks to</p>
          <h1 className="text-4xl font-bold md:text-6xl">People behind the prototype</h1>
          <p className="mt-5 max-w-2xl text-sm leading-7 text-white/58">
            SolWear is being built in public: firmware, Android, site, service tooling,
            hardware iterations, and a lot of late-night NFC testing.
          </p>
          <div className="mt-10 grid gap-3 md:grid-cols-2">
            {people.map((person) => (
              <div key={person} className="border border-white/10 bg-white/[0.04] p-5 text-sm text-white/75">
                {person}
              </div>
            ))}
          </div>
          <a className="focus-ring mt-10 inline-flex min-h-11 items-center bg-white px-5 py-3 text-sm font-semibold text-black" href="/pinboard">
            Add an idea
          </a>
        </section>
      </main>
      <Footer />
    </>
  );
}
