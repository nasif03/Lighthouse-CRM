import { PropsWithChildren } from 'react';

export default function Card({ children }: PropsWithChildren) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
      {children}
    </div>
  );
}

export function CardHeader({ children }: PropsWithChildren) {
  return <div className="px-5 py-3.5 border-b border-gray-200 font-semibold tracking-tight">{children}</div>;
}

export function CardContent({ children }: PropsWithChildren) {
  return <div className="p-5">{children}</div>;
}


