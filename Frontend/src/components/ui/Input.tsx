import { InputHTMLAttributes, forwardRef } from 'react';
import { clsx } from 'clsx';

type Props = InputHTMLAttributes<HTMLInputElement>;

const Input = forwardRef<HTMLInputElement, Props>(({ className, ...rest }, ref) => {
  return (
    <input
      ref={ref}
      className={clsx('h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm placeholder:text-gray-400', className)}
      {...rest}
    />
  );
});

export default Input;


