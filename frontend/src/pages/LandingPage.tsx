import { useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useGSAP } from '@gsap/react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { ArrowRight, CupSoda, Fuel, Landmark, Pizza, ReceiptText, Users } from 'lucide-react'
import { DrawablyButton, DrawablyDivider, DrawablyUnderline } from 'drawably/react'

const platePath = (name: string) => `/assets/plates/${name}.png`

const paperTexture = platePath('paper-texture')
const upperMapMarks = platePath('upper-map-marks')
const trailSign = platePath('trail-sign')
gsap.registerPlugin(ScrollTrigger, useGSAP)

const PHONE_NOTES = [
  ['Jamie', 'Luna Pizza', '$84.20'], ['Noor', 'Drinks + plates', '$46.80'],
  ['Eli', 'Gas', '$52.40'], ['Mina', 'Movie tickets', '$38.00'], ['You', 'Snacks', '$21.50'],
] as const

export function LandingPage() {
  const root = useRef<HTMLElement>(null)
  const navigate = useNavigate()

  useGSAP(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const frame = root.current?.querySelector<HTMLElement>('.comp-frame')
    gsap.set('.route-arrival', { autoAlpha: 0, scale: 0.65, transformOrigin: 'center' })

    const trailTimeline = gsap.timeline({
      scrollTrigger: {
        trigger: '.trail-scroll',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 0.45,
        invalidateOnRefresh: true,
      },
    })
    trailTimeline
      .to('.comp-frame', {
        y: () => Math.min(0, window.innerHeight - (frame?.offsetHeight ?? window.innerHeight)),
        duration: 4,
        ease: 'none',
      }, 0)
      .to('.map-route-path--jamie', { attr: { 'stroke-dashoffset': 0 }, duration: 1.25, ease: 'none' }, 0)
      .to('.map-route-path--noor', { attr: { 'stroke-dashoffset': 0 }, duration: 1.25, ease: 'none' }, 0.55)
      .to('.map-route-path--eli', { attr: { 'stroke-dashoffset': 0 }, duration: 1.25, ease: 'none' }, 1.1)
      .to('.route-arrival', { autoAlpha: 1, scale: 1, duration: 0.35, stagger: 0.08, ease: 'power2.out' }, 2.35)

    const phoneTimeline = gsap.timeline({
      scrollTrigger: { trigger: '.scroll-story', start: 'top top', end: 'bottom bottom', scrub: 0.6, pin: '.phone-stage' },
    })
    phoneTimeline
      .fromTo('.phone-card', { x: 0, y: 0, rotation: 0, scale: 2.8, opacity: 0 }, { x: 0, y: 0, rotation: (index) => [-7, 8, 5, -8, 2][index], scale: 1, opacity: 1, stagger: 0.08, duration: 1.4, ease: 'power2.out' })
      .fromTo('.orbit-line', { scale: 0.55, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.65 }, '<0.55')
      .to('.phone-orbit', { rotation: 26, duration: 1.2, ease: 'none' })
      .fromTo('.orbit-copy', { y: 26, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5 }, '<0.5')
    return () => {
      trailTimeline.scrollTrigger?.kill()
      phoneTimeline.scrollTrigger?.kill()
    }
  }, { scope: root })

  return (
    <main className="landing" ref={root}>
      <section className="trail-scroll">
        <div className="trail-stage">
          <section className="comp-frame" style={{ backgroundImage: `url(${paperTexture})` }} aria-labelledby="landing-title">
        <header className="hero-header">
          <Link className="wordmark" to="/">SettleUp</Link>
          <nav className="hero-nav" aria-label="Primary navigation"><Link to="/auth">Sign in</Link><button className="menu-button" type="button" aria-label="Jump to how SettleUp works" onClick={() => document.querySelector('#how-it-works')?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })}><span aria-hidden="true" /><span aria-hidden="true" /><span aria-hidden="true" /></button></nav>
          <DrawablyDivider roughness={0.8} boil={0.14} />
        </header>
        <div className="hero-copy">
          <h1 id="landing-title">We get each<br />other next time.</h1>
          <p>Start with what actually happened.</p>
          <DrawablyButton className="connect-button" variant="solid" roughness={0.65} boil={0.16} onClick={() => navigate('/auth?intent=connect')}>
            Connect a bank <ArrowRight aria-hidden="true" />
          </DrawablyButton>
        </div>
        <div className="friend-map" aria-label="Jamie, Noor, and Eli each cover a different purchase before their trails meet">
          <img className="map-plate map-plate--upper" src={upperMapMarks} alt="" />
          <img className="map-plate map-plate--sign" src={trailSign} alt="" />
          <svg className="friend-map-routes" viewBox="0 0 1000 820" preserveAspectRatio="none" aria-hidden="true">
            <g className="map-landmarks">
              <path d="M 40 500 q 28 -23 56 0 t 56 0" />
              <path d="M 42 526 q 28 -23 56 0 t 56 0" />
              <path d="M 786 500 l 34 -44 23 29 34 -49 48 64" />
              <path className="map-landmark-trail" d="M 45 625 C 135 580, 185 670, 270 620 S 390 610, 420 645" />
              <circle cx="175" cy="620" r="23" />
              <path className="map-tree" d="M 886 275 l -15 22 h 10 l -17 22 h 17 v 29 M 936 318 l -13 19 h 9 l -15 20 h 15 v 25" />
            </g>
            <path className="map-route-path map-route-path--jamie" pathLength="1" strokeDasharray="1" strokeDashoffset="1" d="M 150 240 C 150 390, 250 470, 446 620" />
            <path className="map-route-path map-route-path--noor" pathLength="1" strokeDasharray="1" strokeDashoffset="1" d="M 500 330 C 490 455, 480 545, 490 620" />
            <path className="map-route-path map-route-path--eli" pathLength="1" strokeDasharray="1" strokeDashoffset="1" d="M 845 240 C 830 395, 710 485, 544 620" />
            <path className="route-arrival" d="M 417 599 L 446 620 L 430 588" />
            <path className="route-arrival" d="M 476 589 L 490 620 L 507 590" />
            <path className="route-arrival" d="M 572 592 L 544 620 L 578 608" />
          </svg>

          <article className="friend-stop friend-stop--jamie">
            <div className="map-person map-person--jamie" aria-hidden="true"><span>J</span></div>
            <span className="name-slip">Jamie</span>
            <div className="purchase-slip"><Pizza aria-hidden="true" /><span>Luna Pizza</span><strong>$84.20</strong></div>
          </article>
          <article className="friend-stop friend-stop--noor">
            <div className="map-person map-person--noor" aria-hidden="true"><span>N</span></div>
            <span className="name-slip">Noor</span>
            <div className="purchase-slip"><CupSoda aria-hidden="true" /><span>Drinks + plates</span><strong>$46.80</strong></div>
          </article>
          <article className="friend-stop friend-stop--eli">
            <div className="map-person map-person--eli" aria-hidden="true"><span>E</span></div>
            <span className="name-slip">Eli</span>
            <div className="purchase-slip"><Fuel aria-hidden="true" /><span>Gas + utilities</span><strong>$112.40</strong></div>
          </article>
          <div className="group-stop"><div className="group-faces-mini" aria-hidden="true"><span>J</span><span>N</span><span>E</span></div><strong>The group evens out</strong></div>
        </div>
          </section>
        </div>
      </section>

      <section className="scroll-story" aria-labelledby="story-title">
        <div className="phone-stage">
          <p className="stage-note">Keep scrolling — the whole circle is here.</p>
          <div className="phone-orbit" aria-hidden="true">
            <div className="orbit-line" />
            {PHONE_NOTES.map(([name, item, amount], index) => (
              <div className={`phone-card phone-card--${index + 1}`} key={name}>
                <span className="phone-speaker" /><div className="comic-avatar">{name[0]}</div><strong>{name}</strong><small>{item}</small><b>{amount}</b>
              </div>
            ))}
          </div>
          <div className="orbit-copy"><h2 id="story-title">Five phones. One shared trail.</h2><p>Everyone picks up something. SettleUp keeps the bank-backed record and shows when the group is even.</p></div>
        </div>
      </section>

      <section className="landing-explainer" id="how-it-works" aria-labelledby="explainer-title">
        <h2 id="explainer-title">Your transactions become the group’s shared memory.</h2>
        <div className="explainer-steps">
          <article><Landmark /><span>01</span><h3>Connect your bank</h3><p>Bring in real purchases. Nothing relies on someone remembering to log dinner later.</p></article>
          <article><ReceiptText /><span>02</span><h3>Choose what was shared</h3><p>Send pizza, gas, tickets, or groceries to the right friend group in a tap.</p></article>
          <article><Users /><span>03</span><h3>Let next time count</h3><p>See the whole trail, not a fussy receipt-by-receipt IOU tally.</p></article>
        </div>
        <DrawablyButton className="closing-cta" variant="solid" onClick={() => navigate('/auth?intent=connect')}>Start with a transaction <ArrowRight /></DrawablyButton>
        <p className="landing-signoff">Built for friends who say <DrawablyUnderline>“I’ve got the next one.”</DrawablyUnderline></p>
      </section>
    </main>
  )
}
