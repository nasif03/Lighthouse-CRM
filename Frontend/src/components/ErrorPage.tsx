import { useRouteError, isRouteErrorResponse, Link } from 'react-router-dom';

export default function ErrorPage() {
  const error = useRouteError();

  let title = 'Something went wrong';
  let message = 'An unexpected error occurred. Please try again.';

  if (isRouteErrorResponse(error)) {
    title = `Error ${error.status}`;
    message = error.statusText || message;
  } else if (error instanceof Error) {
    message = error.message;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-lg w-full bg-white border border-gray-200 shadow-sm rounded-lg p-8 text-center space-y-4">
        <div className="text-3xl font-semibold text-gray-900">{title}</div>
        <p className="text-gray-600">{message}</p>
        <p className="text-sm text-gray-500">
          If the issue persists, try refreshing the page or contact support.
        </p>
        <div className="flex justify-center gap-3">
          <button
            className="px-4 py-2 rounded-md border border-gray-200 text-gray-700 hover:bg-gray-100"
            onClick={() => window.location.reload()}
          >
            Refresh
          </button>
          <Link
            to="/"
            className="px-4 py-2 rounded-md bg-brand-600 text-white hover:bg-brand-700"
          >
            Go to dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}

