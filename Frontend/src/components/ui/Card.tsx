import { PropsWithChildren } from 'react';

export default function Card({ children }: PropsWithChildren) {
  return (
    <div className="rounded border border-gray-200 bg-white">
      {children}
    </div>
  );
}

export function CardHeader({ children }: PropsWithChildren) {
  return <div className="px-4 py-3 border-b border-gray-200 font-medium">{children}</div>;
}

export function CardContent({ children }: PropsWithChildren) {
  return <div className="p-4">{children}</div>;
}


