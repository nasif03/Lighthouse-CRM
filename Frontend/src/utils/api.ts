const API_BASE_URL = 'http://localhost:3000';

// Request cache
const requestCache = new Map<string, { data: any; timestamp: number }>();
const CACHE_TTL = 30000; // 30 seconds

// Abort controllers for request cancellation
const abortControllers = new Map<string, AbortController>();

interface RequestOptions extends RequestInit {
  skipCache?: boolean;
  cacheKey?: string;
}

/**
 * Centralized API client with caching and request cancellation
 */
export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipCache = false, cacheKey, ...fetchOptions } = options;
  const url = `${API_BASE_URL}${endpoint}`;
  const key = cacheKey || `${fetchOptions.method || 'GET'}:${endpoint}`;

  // Cancel previous request with same key
  if (abortControllers.has(key)) {
    abortControllers.get(key)?.abort();
  }

  // Check cache first (only for GET requests)
  if (!skipCache && fetchOptions.method === undefined || fetchOptions.method === 'GET') {
    const cached = requestCache.get(key);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      return cached.data as T;
    }
  }

  // Create new abort controller
  const controller = new AbortController();
  abortControllers.set(key, controller);

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...fetchOptions.headers,
      },
    });

    // Remove abort controller on completion
    abortControllers.delete(key);

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.detail || errorData.message || `HTTP ${response.status}`);
    }

    const data = await response.json();

    // Cache GET requests
    if (!skipCache && (fetchOptions.method === undefined || fetchOptions.method === 'GET')) {
      requestCache.set(key, { data, timestamp: Date.now() });
    }

    return data as T;
  } catch (error: any) {
    abortControllers.delete(key);
    
    // Don't throw error if request was aborted
    if (error.name === 'AbortError') {
      throw new Error('Request cancelled');
    }
    
    throw error;
  }
}

/**
 * Get request with authentication
 */
export async function apiGet<T>(
  endpoint: string,
  token: string | null,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'GET',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}

/**
 * POST request with authentication
 */
export async function apiPost<T>(
  endpoint: string,
  token: string | null,
  data?: any,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'POST',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PUT request with authentication
 */
export async function apiPut<T>(
  endpoint: string,
  token: string | null,
  data?: any,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'PUT',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * DELETE request with authentication
 */
export async function apiDelete<T>(
  endpoint: string,
  token: string | null,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'DELETE',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}

/**
 * Clear request cache
 */
export function clearCache(pattern?: string): void {
  if (pattern) {
    for (const key of requestCache.keys()) {
      if (key.includes(pattern)) {
        requestCache.delete(key);
      }
    }
  } else {
    requestCache.clear();
  }
}

/**
 * Cancel all pending requests
 */
export function cancelAllRequests(): void {
  for (const controller of abortControllers.values()) {
    controller.abort();
  }
  abortControllers.clear();
}

