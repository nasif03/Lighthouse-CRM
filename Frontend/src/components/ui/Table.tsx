import { PropsWithChildren, ReactNode } from 'react';

export function Table({ children }: PropsWithChildren) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        {children}
      </table>
    </div>
  );
}

export function THead({ children }: PropsWithChildren) {
  return <thead className="bg-gray-50 text-left text-gray-600">{children}</thead>;
}

export function TBody({ children }: PropsWithChildren) {
  return <tbody className="divide-y divide-gray-200">{children}</tbody>;
}

export function TR({ children }: PropsWithChildren) {
  return <tr className="hover:bg-gray-50/60">{children}</tr>;
}

export function TH({ children }: { children: ReactNode }) {
  return <th className="px-3 py-2 font-medium">{children}</th>;
}

export function TD({ children }: { children: ReactNode }) {
  return <td className="px-3 py-2">{children}</td>;
}


