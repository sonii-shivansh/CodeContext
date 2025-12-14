// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Navbar background on scroll
const navbar = document.querySelector('.navbar');
let lastScroll = 0;

window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset;

    if (currentScroll > 100) {
        navbar.style.background = 'rgba(10, 10, 15, 0.95)';
        navbar.style.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.4)';
    } else {
        navbar.style.background = 'rgba(10, 10, 15, 0.8)';
        navbar.style.boxShadow = 'none';
    }

    lastScroll = currentScroll;
});

// Intersection Observer for animations
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -100px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Observe feature cards
document.querySelectorAll('.feature-card').forEach((card, index) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(30px)';
    card.style.transition = `all 0.6s ease-out ${index * 0.1}s`;
    observer.observe(card);
});

// Observe install steps
document.querySelectorAll('.install-step').forEach((step, index) => {
    step.style.opacity = '0';
    step.style.transform = 'translateY(30px)';
    step.style.transition = `all 0.6s ease-out ${index * 0.15}s`;
    observer.observe(step);
});

// Terminal typing effect (optional enhancement)
const terminalLines = document.querySelectorAll('.terminal-line.output');
terminalLines.forEach((line, index) => {
    line.style.opacity = '0';
    setTimeout(() => {
        line.style.transition = 'opacity 0.3s ease-in';
        line.style.opacity = '1';
    }, 1000 + (index * 200));
});

// Hotspot bar animation on scroll
const hotspotObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const bars = entry.target.querySelectorAll('.hotspot-bar');
            bars.forEach((bar, index) => {
                setTimeout(() => {
                    bar.style.transition = 'width 1s ease-out';
                    const width = bar.style.width;
                    bar.style.width = '0';
                    setTimeout(() => {
                        bar.style.width = width;
                    }, 50);
                }, index * 150);
            });
        }
    });
}, { threshold: 0.5 });

const hotspotList = document.querySelector('.hotspot-list');
if (hotspotList) {
    hotspotObserver.observe(hotspotList);
}

// Add hover effect to code blocks for copy functionality
document.querySelectorAll('.code-block').forEach(block => {
    block.style.position = 'relative';

    const copyButton = document.createElement('button');
    copyButton.innerHTML = '📋';
    copyButton.style.cssText = `
        position: absolute;
        top: 8px;
        right: 8px;
        background: rgba(99, 102, 241, 0.2);
        border: 1px solid rgba(99, 102, 241, 0.3);
        color: #6366f1;
        padding: 0.5rem;
        border-radius: 6px;
        cursor: pointer;
        opacity: 0;
        transition: all 0.3s ease;
        font-size: 1rem;
    `;

    block.appendChild(copyButton);

    block.addEventListener('mouseenter', () => {
        copyButton.style.opacity = '1';
    });

    block.addEventListener('mouseleave', () => {
        copyButton.style.opacity = '0';
    });

    copyButton.addEventListener('click', () => {
        const code = block.querySelector('code').textContent;
        navigator.clipboard.writeText(code).then(() => {
            copyButton.innerHTML = '✅';
            setTimeout(() => {
                copyButton.innerHTML = '📋';
            }, 2000);
        });
    });
});

// Parallax effect for gradient orbs
window.addEventListener('mousemove', (e) => {
    const orbs = document.querySelectorAll('.gradient-orb');
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;

    orbs.forEach((orb, index) => {
        const speed = (index + 1) * 20;
        const xMove = (x - 0.5) * speed;
        const yMove = (y - 0.5) * speed;
        orb.style.transform = `translate(${xMove}px, ${yMove}px)`;
    });
});

// Add active state to navigation links based on scroll position
const sections = document.querySelectorAll('section[id]');
const navLinks = document.querySelectorAll('.nav-link[href^="#"]');

window.addEventListener('scroll', () => {
    let current = '';

    sections.forEach(section => {
        const sectionTop = section.offsetTop;
        const sectionHeight = section.clientHeight;
        if (pageYOffset >= sectionTop - 200) {
            current = section.getAttribute('id');
        }
    });

    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${current}`) {
            link.classList.add('active');
        }
    });
});

// Console Easter egg
console.log('%c🧠 CodeContext', 'font-size: 24px; font-weight: bold; background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;');
console.log('%cInterested in how this works? Check out our GitHub: https://github.com/sonii-shivansh/CodeContext', 'font-size: 14px; color: #6366f1;');
console.log('%cWe\'re open source! Contributions welcome 🚀', 'font-size: 12px; color: #94a3b8;');
