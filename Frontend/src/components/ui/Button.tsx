import { ButtonHTMLAttributes } from 'react';
import { clsx } from 'clsx';

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost';
};

export default function Button({ className, variant = 'primary', ...rest }: Props) {
  const base = 'inline-flex items-center justify-center rounded text-sm font-medium h-9 px-3 transition-colors';
  const styles = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700',
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-900',
    ghost: 'bg-transparent hover:bg-gray-100 text-gray-900',
  } as const;
  return <button className={clsx(base, styles[variant], className)} {...rest} />;
}


