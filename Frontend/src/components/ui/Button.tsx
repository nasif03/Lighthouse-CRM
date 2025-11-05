import { ButtonHTMLAttributes } from 'react';
import { clsx } from 'clsx';

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost';
};

export default function Button({ className, variant = 'primary', ...rest }: Props) {
  const base = 'inline-flex items-center justify-center rounded-md text-sm font-semibold h-9 px-3 transition-colors shadow-sm';
  const styles = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600',
    secondary: 'bg-white border border-gray-200 hover:bg-gray-50 text-gray-900',
    ghost: 'bg-transparent hover:bg-gray-100 text-gray-900',
  } as const;
  return <button className={clsx(base, styles[variant], className)} {...rest} />;
}


