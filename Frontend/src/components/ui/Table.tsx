import { PropsWithChildren, ReactNode, HTMLAttributes } from 'react';

export function Table({ children, className }: PropsWithChildren<{ className?: string }>) {
  return (
    <div className="overflow-x-auto">
      <table className={className || "w-full text-sm"}>
        {children}
      </table>
    </div>
  );
}

export function THead({ children }: PropsWithChildren) {
  return <thead className="bg-gray-50 text-left text-gray-700">{children}</thead>;
}

export function TBody({ children }: PropsWithChildren) {
  return <tbody className="divide-y divide-gray-200">{children}</tbody>;
}

export function TR({ children, className, onClick }: PropsWithChildren<HTMLAttributes<HTMLTableRowElement>>) {
  return <tr className={className || "hover:bg-gray-50"} onClick={onClick}>{children}</tr>;
}

export function TH({ children, className }: { children: ReactNode; className?: string }) {
  return <th className={className || "px-3 py-2 font-medium"}>{children}</th>;
}

export function TD({ children, className, onClick, colSpan }: { children: ReactNode; className?: string; onClick?: (e: React.MouseEvent) => void; colSpan?: number }) {
  return <td className={className || "px-3 py-2"} onClick={onClick} colSpan={colSpan}>{children}</td>;
}