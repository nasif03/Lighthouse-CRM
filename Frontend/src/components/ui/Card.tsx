import { PropsWithChildren } from 'react';
import { clsx } from 'clsx';

interface CardProps extends PropsWithChildren {
  className?: string;
}

export default function Card({ children, className }: CardProps) {
  return (
    <div className={clsx("rounded-lg border border-gray-200 bg-white shadow-sm", className)}>
      {children}
    </div>
  );
}

interface CardHeaderProps extends PropsWithChildren {
  className?: string;
}

export function CardHeader({ children, className }: CardHeaderProps) {
  return <div className={clsx("px-5 py-3.5 border-b border-gray-200 font-semibold tracking-tight", className)}>{children}</div>;
}

interface CardContentProps extends PropsWithChildren {
  className?: string;
}

export function CardContent({ children, className }: CardContentProps) {
  return <div className={clsx("p-5", className)}>{children}</div>;
}


