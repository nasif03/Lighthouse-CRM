# Lighthouse CRM - Frontend

A modern, multi-tenant CRM web application built with React, TypeScript, and Tailwind CSS. Designed for business owners to manage sales, marketing, customer support, and administration with an integrated messaging system.

## 🚀 Tech Stack

- **React 18.3** - UI library
- **TypeScript 5.6** - Type safety
- **Vite 5.4** - Build tool and dev server
- **React Router 6.26** - Client-side routing
- **Zustand 4.5** - State management
- **Tailwind CSS 3.4** - Utility-first CSS framework
- **clsx** - Conditional class names

## 📁 Project Structure

```
Frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── ui/              # Base UI components (Button, Card, Input, Table, Modal, Tabs)
│   │   ├── inbox/           # Inbox/messaging components
│   │   ├── icons.tsx        # SVG icon components
│   │   └── TenantSwitcher.tsx
│   ├── layouts/             # Layout components
│   │   └── AppLayout.tsx    # Main application layout (sidebar + header + inbox)
│   ├── pages/               # Page components
│   │   ├── Dashboard.tsx
│   │   ├── Leads.tsx
│   │   ├── Contacts.tsx
│   │   ├── Deals.tsx
│   │   ├── Campaigns.tsx
│   │   ├── Segments.tsx
│   │   ├── Templates.tsx
│   │   ├── Analytics.tsx
│   │   ├── Support.tsx
│   │   ├── Administration.tsx
│   │   ├── Settings.tsx
│   │   └── Login.tsx
│   ├── store/               # Zustand state stores
│   │   ├── authStore.ts     # Authentication state
│   │   ├── tenantStore.ts   # Multi-tenant state
│   │   └── inboxStore.ts   # Inbox/conversations state
│   ├── router.tsx           # React Router configuration
│   └── main.tsx             # Application entry point
├── styles/
│   └── index.css            # Global styles and Tailwind imports
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

## ✨ Features

### Core CRM Features

Organized into business-focused sections:

#### Overview
- **Dashboard** - Overview with key metrics (Active Campaigns, New Leads, Email Open Rate)

#### Sales
- **Leads Management** - View, search, and manage leads with status tracking
- **Contacts** - Contact management with company information
- **Deals** - Deal/Sales pipeline management with value, stage, and probability tracking

#### Marketing
- **Campaigns** - Marketing campaign management with status tracking (Active/Completed tabs)
- **Segments** - Customer segmentation with contact counts
- **Templates** - Email template management
- **Analytics** - Campaign performance and lead source analytics

#### Customer Support
- **Support Tickets** - Customer support ticket management with priority levels and status tracking

#### Administration
- **Administration** - Business administration tools (Users, Roles & Permissions, Workspace Settings, Integrations)
- **Settings** - Workspace configuration (multi-tenant)

### Multi-Tenant Support

- **Tenant Switcher** in header - Switch between different tenants
- Tenant-aware state management via Zustand
- UI displays current active tenant in inbox header
- Designed for data isolation per tenant

### Inbox & Messaging System

- **Persistent Inbox Panel** - Always visible on the right side (420px width)
- **Conversation List** - View all conversations with unread badges and timestamps
- **Conversation View** - Full chat interface with:
  - Back button to return to inbox list
  - Participant avatar, name, and online status
  - Message history with sent/received differentiation
  - Message input with Enter key support
  - Auto-scroll to latest messages
- **Call Integration** - Call button ready for VoIP integration (open-source VoIP)
- **Multi-tenant** - Conversations are tenant-aware

### UI Components

A comprehensive design system with reusable components:

- **Button** - Primary, secondary, and ghost variants
- **Input** - Text input with focus states
- **Card** - Container with header and content sections
- **Table** - Data table with head, body, rows, cells
- **Modal** - Overlay modal dialog
- **Tabs** - Tabbed interface component

### Design System

- **Brand Color**: Light blue (#2768F5) - `brand-600` in Tailwind config
- **Color Palette**: Full brand color scale (50-900)
- **Typography**: System font stack
- **Spacing**: Tailwind's default spacing scale
- **Icons**: Custom SVG icon components for navigation

## 🎨 Design Patterns

- **KISS** (Keep It Simple, Stupid) - Simple, straightforward component structure
- **DRY** (Don't Repeat Yourself) - Reusable UI components and stores
- **SOLID Principles** - Single responsibility components, clear separation of concerns
- **Component Composition** - Small, focused components composed into larger features

## 🗂️ State Management

### Zustand Stores

1. **authStore** - User authentication state
   - Current user
   - Login/logout functions

2. **tenantStore** - Multi-tenant state
   - List of tenants
   - Active tenant ID
   - Tenant switching functionality

3. **inboxStore** - Messaging state
   - Conversations list
   - Active conversation
   - Message management (add, mark as read)
   - Mock data for development

## 🛣️ Routing

React Router v6 with the following routes:

### Overview
- `/` - Dashboard (default)

### Sales
- `/leads` - Leads management
- `/contacts` - Contacts
- `/deals` - Deals/Sales pipeline

### Marketing
- `/campaigns` - Campaigns
- `/segments` - Segments
- `/templates` - Templates
- `/analytics` - Analytics

### Customer Support
- `/support` - Support Tickets

### Administration
- `/administration` - Administration
- `/settings` - Settings

### Authentication
- `/login` - Login page (outside main layout)

All routes except `/login` are wrapped in `AppLayout` which provides:
- Sidebar navigation organized by business function (Sales and Marketing separated)
- Header spanning main content and inbox area with fixed-width title, tenant switcher, and user info
- Right-side inbox panel (persistent, 420px width)

## 📦 Layout Structure

```
┌─────────────────────────────────────────────────────────────┐
│ Sidebar │  Header (title, tenant, user, logout)  │ Inbox │
│(280px)  │  (spans main + inbox columns)         │(420px)│
│         ├─────────────────────────────────────────┤       │
│         │                                           │       │
│         │      Main Content Area                    │       │
│         │      (Dynamic Page Content)               │       │
│         │                                           │       │
└─────────────────────────────────────────────────────────────┘
```

### Sidebar Navigation Structure

The sidebar is organized into clear sections for business owners:

1. **Overview** - Dashboard
2. **Sales** - Leads, Contacts, Deals
3. **Marketing** - Campaigns, Segments, Templates, Analytics
4. **Customer Support** - Support Tickets
5. **Administration** - Administration, Settings

Each section has a header label with grouped navigation items below.

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
cd Frontend
npm install
```

### Development

```bash
npm run dev
```

The app will start on `http://localhost:5173`

### Build

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## 🎯 Key Features Implementation

### Inbox Toggle Behavior

- **Initial State**: Shows inbox sidebar (conversation list)
- **Click Conversation**: Hides sidebar, shows full conversation view
- **Back Button**: Returns to inbox sidebar
- **Persistent**: Inbox panel visible on all pages

### Multi-Tenant Awareness

- Tenant ID stored in Zustand store
- Displayed in inbox header
- Ready for backend integration with tenant-scoped data

### Mock Data

Currently using mock data for:
- Leads (8 sample leads)
- Contacts (2 sample contacts)
- Deals (2 sample deals with value, stage, probability)
- Campaigns (2 sample campaigns)
- Segments (2 sample segments)
- Templates (2 sample templates)
- Support Tickets (2 sample tickets with priority and status)
- Conversations (3 sample conversations with Alex, Sarah, Mike)

## 🔧 Configuration

### Tailwind CSS

Custom brand colors configured in `tailwind.config.js`:
- Primary brand color: `#2768F5` (brand-600)
- Full color scale from 50 (lightest) to 900 (darkest)

### Vite

- Dev server on port 5173
- Auto-open browser on start
- React plugin configured

### TypeScript

- Strict mode enabled
- Path aliases configured for cleaner imports:
  - `@components/*`
  - `@pages/*`
  - `@layouts/*`
  - `@store/*`
  - `@utils/*`

## 📝 Notes

- **No Backend Integration Yet**: All data is mock data stored in Zustand stores
- **Business Owner Focus**: Sidebar organized by business function for easy navigation
- **Integrated Inbox**: Inbox panel seamlessly integrated into main layout background
- **Header Coverage**: Header spans both main content and inbox areas for unified interface
- **VoIP Integration Ready**: Call buttons are in place, ready for open-source VoIP library integration
- **Responsive Design**: Basic responsive breakpoints, optimized for desktop-first
- **Accessibility**: Semantic HTML and ARIA labels where appropriate

## 🔮 Future Enhancements

- [ ] Backend API integration
- [ ] Real-time messaging (WebSocket)
- [ ] VoIP call functionality
- [ ] Advanced search and filtering
- [ ] Data visualization charts
- [ ] Export functionality
- [ ] Mobile responsive improvements
- [ ] Dark mode support
- [ ] Internationalization (i18n)

## 📄 License

Private project - CSE327

---

Built with ❤️ for Lighthouse CRM

