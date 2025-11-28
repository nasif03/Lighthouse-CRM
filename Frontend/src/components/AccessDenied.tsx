import Card, { CardContent } from './ui/Card';

interface AccessDeniedProps {
  message?: string;
  helpText?: string;
}

export default function AccessDenied({ 
  message = "You do not have permission to access this page.", 
  helpText 
}: AccessDeniedProps) {
  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="p-8 text-center">
          <div className="text-red-600 mb-4">
            <svg className="w-16 h-16 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Access Denied</h2>
          <p className="text-gray-600 mb-4">{message}</p>
          {helpText && (
            <p className="text-sm text-gray-500">{helpText}</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
