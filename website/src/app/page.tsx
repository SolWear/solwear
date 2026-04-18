import Nav from "@/components/Nav";
import Hero from "@/components/Hero";
import Features from "@/components/Features";
import WatchShowcase from "@/components/WatchShowcase";
import About from "@/components/About";
import Preorder from "@/components/Preorder";
import Links from "@/components/Links";
import Footer from "@/components/Footer";

export default function Home() {
  return (
    <>
      <Nav />
      <main>
        <Hero />
        <Features />
        <WatchShowcase />
        <About />
        <Preorder />
        <Links />
      </main>
      <Footer />
    </>
  );
}
