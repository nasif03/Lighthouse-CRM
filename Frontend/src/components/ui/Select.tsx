import { SelectHTMLAttributes, forwardRef } from 'react';
import { clsx } from 'clsx';

type Props = SelectHTMLAttributes<HTMLSelectElement>;

const Select = forwardRef<HTMLSelectElement, Props>(({ className, children, ...rest }, ref) => {
  return (
    <select
      ref={ref}
      className={clsx('h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm', className)}
      {...rest}
    >
      {children}
    </select>
  );
});

Select.displayName = 'Select';

export default Select;

