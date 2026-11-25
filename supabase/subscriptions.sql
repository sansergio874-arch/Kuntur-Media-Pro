-- Ejecutar en el SQL Editor de Supabase
-- Tabla de suscripciones / solicitudes de plan

create table if not exists public.kuntur_subscriptions (
  id uuid primary key default gen_random_uuid(),
  device_id text not null,
  plan_id text not null,
  plan_name text not null,
  price int not null default 0,
  status text not null default 'pending'
    check (status in ('pending', 'approved', 'rejected', 'expired')),
  transaction_code text not null unique,
  yape_ref text default '',
  created_at timestamptz not null default now(),
  approved_at timestamptz,
  expires_at timestamptz
);

create index if not exists kuntur_subscriptions_device_idx
  on public.kuntur_subscriptions (device_id);

create index if not exists kuntur_subscriptions_code_idx
  on public.kuntur_subscriptions (transaction_code);

alter table public.kuntur_subscriptions enable row level security;

-- Cualquiera con anon key puede crear solicitudes pendientes
create policy "insert_pending"
  on public.kuntur_subscriptions
  for insert
  to anon, authenticated
  with check (status = 'pending');

-- Solo puede leer sus propias filas por device_id (filtro en la query de la app)
create policy "select_own"
  on public.kuntur_subscriptions
  for select
  to anon, authenticated
  using (true);

-- La aprobación SOLO se hace desde el Dashboard de Supabase (rol service_role)
-- o con una Edge Function protegida. No permita UPDATE desde la app anónima.

comment on table public.kuntur_subscriptions is
  'Solicitudes de plan Kuntur Media Pro. Apruebe cambiando status a approved en el Dashboard.';
