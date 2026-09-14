import { useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useGSAP } from '@gsap/react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { ArrowRight, Landmark, ReceiptText, Users } from 'lucide-react'
import { DrawablyButton, DrawablyDivider, DrawablyUnderline } from 'drawably/react'

const platePath = (name: string) => `/assets/plates/${name}.png`

const paperTexture = platePath('paper-texture')
const upperMapMarks = platePath('upper-map-marks')
const jamieStory = platePath('jamie-story')
const noorStory = platePath('noor-story')
const eliStory = platePath('eli-story')
const routeConvergence = platePath('route-convergence')
const groupMarker = platePath('group-marker')
const trailSign = platePath('trail-sign')

gsap.registerPlugin(ScrollTrigger, useGSAP)

const PLATES = [
  ['upper-map-marks', upperMapMarks],
  ['jamie-story', jamieStory], ['noor-story', noorStory], ['eli-story', eliStory],
  ['route-convergence', routeConvergence], ['group-marker', groupMarker], ['trail-sign', trailSign],
] as const

const PHONE_NOTES = [
  ['Jamie', 'Luna Pizza', '$84.20'], ['Noor', 'Drinks + plates', '$46.80'],
  ['Eli', 'Gas', '$52.40'], ['Mina', 'Movie tickets', '$38.00'], ['You', 'Snacks', '$21.50'],
] as const

export function LandingPage() {
  const root = useRef<HTMLElement>(null)
  const navigate = useNavigate()

  useGSAP(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const routeReveal = gsap.fromTo('.plate--route-convergence',
      { clipPath: 'inset(0 0 100% 0)' },
      { clipPath: 'inset(0 0 0% 0)', ease: 'none', scrollTrigger: { trigger: '.comp-frame', start: '30% top', end: 'bottom top', scrub: 0.45 } },
    )
    const timeline = gsap.timeline({
      scrollTrigger: { trigger: '.scroll-story', start: 'top top', end: 'bottom bottom', scrub: 0.6, pin: '.phone-stage' },
    })
    timeline
      .fromTo('.phone-card', { x: 0, y: 0, rotation: 0, scale: 2.8, opacity: 0 }, { x: 0, y: 0, rotation: (index) => [-7, 8, 5, -8, 2][index], scale: 1, opacity: 1, stagger: 0.08, duration: 1.4, ease: 'power2.out' })
      .fromTo('.orbit-line', { scale: 0.55, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.65 }, '<0.55')
      .to('.phone-orbit', { rotation: 26, duration: 1.2, ease: 'none' })
      .fromTo('.orbit-copy', { y: 26, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5 }, '<0.5')
    return () => { routeReveal.scrollTrigger?.kill(); timeline.scrollTrigger?.kill() }
  }, { scope: root })

  return (
    <main className="landing" ref={root}>
      <section className="comp-frame" style={{ backgroundImage: `url(${paperTexture})` }} aria-labelledby="landing-title">
        {PLATES.map(([id, src]) => <img key={id} className={`plate plate--${id}`} src={src} alt="" />)}
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
        <span className="map-label label-jamie-name">Jamie</span><span className="map-label label-jamie-item">Luna Pizza</span><strong className="map-amount label-jamie-amount">$84.20</strong>
        <span className="map-label label-noor-name">Noor</span><span className="map-label label-noor-item">Drinks + plates</span><strong className="map-amount label-noor-amount">$46.80</strong>
        <span className="map-label label-eli-name">Eli</span><span className="map-label label-eli-item">Gas + utilities</span><strong className="map-amount label-eli-amount">$112.40</strong>
        <span className="map-label label-group">The group evens out</span><span className="map-label label-trail">Follow the trail</span>
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
