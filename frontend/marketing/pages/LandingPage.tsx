'use client';

import { Hero } from "../Hero";
import { RoleCards } from "../RoleCards";
import { HowItWorks } from "../HowItWorks";
import { FeaturesGrid } from "../FeaturesGrid";
import { ProductShowcase } from "../ProductShowcase";
import { Testimonials } from "../Testimonials";
import { CTASection } from "../CTASection";
import { Footer } from "../Footer";

export function LandingPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
      <Hero />
      <RoleCards />
      <HowItWorks />
      <FeaturesGrid />
      <ProductShowcase />
      <Testimonials />
      <CTASection />
      <Footer />
    </div>
  );
}
