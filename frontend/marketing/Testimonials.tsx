import { Star, Quote } from "lucide-react";
import { ImageWithFallback } from "./figma/ImageWithFallback";

export function Testimonials() {
  const testimonials = [
    {
      name: "Emma Rodriguez",
      role: "Patient",
      avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop",
      quote: "HealthLink+ made booking appointments so easy. I love that I can access my medical history anytime and join video calls with one click.",
      rating: 5
    },
    {
      name: "Dr. James Wilson",
      role: "Cardiologist",
      avatar: "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=200&h=200&fit=crop",
      quote: "Managing multiple clinics used to be chaotic. Now I can see all my appointments, patient records, and video consults in one secure platform.",
      rating: 5
    },
    {
      name: "Sarah Chen",
      role: "Healthcare Administrator",
      avatar: "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&h=200&fit=crop",
      quote: "The admin dashboard gives us complete visibility. We can monitor appointments, track OTP verifications, and manage our entire staff efficiently.",
      rating: 5
    }
  ];

  return (
    <section className="py-20 lg:py-28 bg-gradient-to-b from-white to-slate-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl text-slate-900 mb-4">
            Trusted Across Healthcare Teams
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            See what patients, doctors, and administrators are saying about HealthLink+.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {testimonials.map((testimonial, index) => (
            <div
              key={index}
              className="bg-white rounded-2xl p-8 shadow-lg border border-slate-200 hover:shadow-xl transition-shadow duration-300"
            >
              {/* Quote icon */}
              <div className="mb-6">
                <Quote className="w-10 h-10 text-teal-500 opacity-50" />
              </div>

              {/* Rating */}
              <div className="flex gap-1 mb-4">
                {[...Array(testimonial.rating)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 fill-amber-400 text-amber-400" />
                ))}
              </div>

              {/* Quote */}
              <p className="text-slate-700 mb-6 leading-relaxed">
                "{testimonial.quote}"
              </p>

              {/* Author */}
              <div className="flex items-center gap-4 pt-6 border-t border-slate-200">
                <ImageWithFallback
                  src={testimonial.avatar}
                  alt={testimonial.name}
                  className="w-12 h-12 rounded-full object-cover"
                />
                <div>
                  <p className="text-slate-900">{testimonial.name}</p>
                  <p className="text-sm text-slate-600">{testimonial.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Trust metrics */}
        <div className="mt-16 grid grid-cols-2 lg:grid-cols-4 gap-8 max-w-4xl mx-auto">
          <div className="text-center">
            <p className="text-3xl sm:text-4xl text-slate-900 mb-2">10,000+</p>
            <p className="text-slate-600">Active Users</p>
          </div>
          <div className="text-center">
            <p className="text-3xl sm:text-4xl text-slate-900 mb-2">500+</p>
            <p className="text-slate-600">Healthcare Providers</p>
          </div>
          <div className="text-center">
            <p className="text-3xl sm:text-4xl text-slate-900 mb-2">50,000+</p>
            <p className="text-slate-600">Appointments Booked</p>
          </div>
          <div className="text-center">
            <p className="text-3xl sm:text-4xl text-slate-900 mb-2">99.9%</p>
            <p className="text-slate-600">Uptime SLA</p>
          </div>
        </div>
      </div>
    </section>
  );
}
