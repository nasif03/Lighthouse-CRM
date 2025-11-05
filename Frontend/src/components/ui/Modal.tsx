import { PropsWithChildren } from 'react';

type Props = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title?: string;
}>;

export default function Modal({ open, onClose, title, children }: Props) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative w-full max-w-lg rounded border border-gray-200 bg-white shadow-xl">
        {title && <div className="px-4 py-3 border-b border-gray-200 font-medium">{title}</div>}
        <div className="p-4">
          {children}
        </div>
      </div>
    </div>
  );
}


