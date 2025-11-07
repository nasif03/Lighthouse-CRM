import { TextareaHTMLAttributes, forwardRef } from 'react';
import { clsx } from 'clsx';

type Props = TextareaHTMLAttributes<HTMLTextAreaElement>;

const Textarea = forwardRef<HTMLTextAreaElement, Props>(({ className, ...rest }, ref) => {
  return (
    <textarea
      ref={ref}
      className={clsx('px-3 py-2 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm placeholder:text-gray-400 resize-y min-h-[100px]', className)}
      {...rest}
    />
  );
});

Textarea.displayName = 'Textarea';

export default Textarea;

